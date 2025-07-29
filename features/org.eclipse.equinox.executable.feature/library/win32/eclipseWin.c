/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at 
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kevin Cornell (Rational Software Corporation)
 *     Holger Voormann - fix for bug 384950 (http://eclip.se/384950)
 *     Gerhard Kreuzer - fix for bug 560428
 *******************************************************************************/

#include "eclipseOS.h"
#include "eclipseUtil.h"
#include "eclipseCommon.h"
#include "eclipseJNI.h"
#include "eclipseShm.h"

#include <windows.h>
#include <commctrl.h>
#include <wincodec.h>
#include <process.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <sys/stat.h>

#ifdef __MINGW32__
#include <stdlib.h>
#endif

extern HWND topWindow;

/* Global Variables */
_TCHAR*  defaultVM     = _T("javaw.exe");
_TCHAR*  consoleVM     = _T("java.exe");
_TCHAR*  vmLibrary 	   = _T("jvm.dll");
_TCHAR*  shippedVMDir  = _T("jre\\bin\\");

/* Define local variables for communicating with running eclipse instance. */
static HANDLE	mutex = 0;
static UINT		findWindowTimeout = 1000;
static UINT_PTR findWindowTimerId = 97;
static UINT		timerCount = 0;
static UINT		openFileTimeout = 60;
static _TCHAR**	openFilePath;

/* Define the window system arguments for the Java VM. */
static _TCHAR*  argVM[] = { NULL };

/* Define the SWT auto-scale setting for the splash screen. */
enum {
	AUTOSCALE_DEFAULT = -1,
	AUTOSCALE_FALSE = -2,
	AUTOSCALE_QUARTER = -3,
	AUTOSCALE_EXACT = -4,
	AUTOSCALE_INTEGER = -5
	/* positive values indicate a literal percentage */
};
static int autoScaleValue = AUTOSCALE_DEFAULT;

/* Define local variables for running the JVM and detecting its exit. */
static HANDLE  jvmProcess     = 0;
static JavaResults* jvmResults = NULL;
static UINT    jvmExitTimeout = 100;
static UINT_PTR jvmExitTimerId = 99;

static void CALLBACK findWindowProc(HWND hwnd, UINT message, UINT idTimer, DWORD dwTime);
static void CALLBACK  detectJvmExit( HWND hwnd, UINT uMsg, UINT id, DWORD dwTime );
static _TCHAR* checkVMRegistryKey(HKEY jrekey, _TCHAR* subKeyName);
static void adjustSearchPath( _TCHAR * vmLibrary );
static _TCHAR* findLib( _TCHAR* command );

/* define default locations in which to find the jvm shared library
 * these are paths relative to the java exe, the shared library is
 * for example jvmLocations[0] + dirSeparator + vmLibrary */
#define MAX_LOCATION_LENGTH 25 /* none of the jvmLocations strings should be longer than this */ 
static const _TCHAR* jvmLocations [] = { _T("j9vm"), _T("..\\jre\\bin\\j9vm"),
										 _T("server"), _T("..\\jre\\bin\\server"),
										 _T("client"), _T("..\\jre\\bin\\client"), 
										 _T("classic"), _T("..\\jre\\bin\\classic"),
										 _T("jrockit"), _T("..\\jre\\bin\\jrockit"),
								 		 NULL };

/* for detecting sun vms */
typedef struct {
	WORD language;
	WORD codepage;
} TRANSLATIONS;

#define PRODUCT_VERSION_KEY _T_ECLIPSE("\\StringFileInfo\\%04x%04x\\ProductVersion")

static void sendOpenFileMessage(HWND window) {
	_TCHAR* id;
	UINT msg;
	int index = 0;
	int size = 0;
	DWORD wParam;
#ifdef WIN64
	DWORDLONG lParam;
#else
	DWORD lParam;
#endif

	/* what's the longest path? */
	while (openFilePath[index] != NULL) {
		int length = _tcslen(openFilePath[index++]);
		if (size <= length)
			size = length + 1;
	}

	createSharedData(&id, size * sizeof(_TCHAR));
	_stscanf(id, _T_ECLIPSE("%lx_%lx"), &wParam, &lParam);
	msg = RegisterWindowMessage(_T("SWT_OPENDOC"));

	index = 0;
	for(index = 0; openFilePath[index] != NULL; index++) {
		/* SendMessage does not return until the message has been processed */
		setSharedData(id, openFilePath[index]);
		SendMessage(window, msg, wParam, lParam);
	}
	destroySharedData(id);
	free(id);
	SetForegroundWindow(window);
}

static HWND findSWTMessageWindow() {
	HWND window = NULL;
	_TCHAR *windowTitle, *windowPrefix, *name;

	windowPrefix = _T("SWT_Window_");
	name = getOfficialName();
	windowTitle = malloc((_tcslen(windowPrefix) + _tcslen(name) + 1) * sizeof(_TCHAR));
	_stprintf(windowTitle, _T_ECLIPSE("%s%s"), windowPrefix, name);
	window = FindWindow(NULL, windowTitle);
	free(windowTitle);
	return window;
}

static void CALLBACK findWindowProc(HWND hwnd, UINT message, UINT idTimer, DWORD dwTime) {
	HWND window = findSWTMessageWindow();
	if (window != NULL) {
		sendOpenFileMessage(window);
		ReleaseMutex(mutex);
		CloseHandle(mutex);
		mutex = 0;
		KillTimer(hwnd, findWindowTimerId);
		return;
	}
	
	/* no window yet, set timer to try again later */
	if (timerCount++ >= openFileTimeout) {
		KillTimer(hwnd, findWindowTimerId);
		ReleaseMutex(mutex);
		CloseHandle(mutex);
		mutex = 0;
	}
}

/* return > 0 if we successfully send a message to another eclipse instance */
int reuseWorkbench(_TCHAR** filePath, int timeout) {
	_TCHAR*   mutexPrefix = _T("SWT_Mutex_");
	_TCHAR*   mutexName, *name;
	DWORD 	  lock;
	HWND 	  window = NULL;

	/* store for later */
	openFilePath = filePath;
	openFileTimeout = timeout;
	
	name = getOfficialName();
	mutexName = malloc((_tcslen(mutexPrefix) + _tcslen(name)  + 1) * sizeof(_TCHAR));
	_stprintf(mutexName, _T_ECLIPSE("%s%s"), mutexPrefix, name);
	mutex = CreateMutex(NULL, FALSE, mutexName);
	free(mutexName);
	if (mutex == NULL) return -1;
	
	//wait for timeout seconds
	lock = WaitForSingleObject(mutex, timeout * 1000);
	if (lock != WAIT_OBJECT_0) {
		/* failed to get the lock before timeout, We won't be reusing an existing eclipse. */
		CloseHandle(mutex);
		mutex = 0;
		return 0;
	}
	
	/* we have the mutex, look for the SWT window */
	window = findSWTMessageWindow();
	if (window != NULL) {
		sendOpenFileMessage(window);
		ReleaseMutex(mutex);
		CloseHandle(mutex);
		mutex = 0;
		return 1; /* success! */
	} 
	
	/* no window, set a timer to look again later */
	if (initWindowSystem(0, NULL) == 0)
		SetTimer( topWindow, findWindowTimerId, findWindowTimeout, findWindowProc );
	
	return 0;
}

HBITMAP loadImage(LPCWSTR pszFilename, int targetDpi) {
	HRESULT hr = S_OK;
	HBITMAP hBitmap = NULL;
	IWICImagingFactory *pFactory = NULL;
	IWICBitmapDecoder *pDecoder = NULL;
	IWICBitmapFrameDecode *pFrame = NULL;
	IWICFormatConverter *pConverter = NULL;
	IWICBitmapScaler *pScaler = NULL;
	IWICBitmapSource *pSource = NULL; /* borrowed, don't deref */

	hr = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
	if (FAILED(hr)) return NULL;
	hr = CoCreateInstance(&CLSID_WICImagingFactory, NULL, CLSCTX_INPROC_SERVER, &IID_IWICImagingFactory, &pFactory);
	if (FAILED(hr)) goto done;

	hr = IWICImagingFactory_CreateDecoderFromFilename(pFactory, pszFilename, NULL, GENERIC_READ, WICDecodeMetadataCacheOnDemand, &pDecoder);
	if (FAILED(hr)) goto done;
	hr = IWICBitmapDecoder_GetFrame(pDecoder, 0, &pFrame);
	if (FAILED(hr)) goto done;
	pSource = (IWICBitmapSource *)pFrame;

	/* Scale the image to the targetDpi, assuming that the source is always 96 dpi. */
	UINT uiWidth = 0, uiHeight = 0;
	hr = IWICBitmapSource_GetSize(pFrame, &uiWidth, &uiHeight);
	if (FAILED(hr)) goto done;
	if (targetDpi != 96) {
		uiWidth = uiWidth * targetDpi / 96;
		uiHeight = uiHeight * targetDpi / 96;
		hr = IWICImagingFactory_CreateBitmapScaler(pFactory, &pScaler);
		if (FAILED(hr)) goto done;
		hr = IWICBitmapScaler_Initialize(pScaler, pSource, uiWidth, uiHeight, WICBitmapInterpolationModeCubic);
		if (FAILED(hr)) goto done;
		pSource = (IWICBitmapSource *)pScaler;
	}

	/* Convert the image to the 24-bit BGR format. */
	WICPixelFormatGUID format;
	hr = IWICBitmapSource_GetPixelFormat(pSource, &format);
	if (FAILED(hr)) goto done;
	if (!IsEqualGUID(&format, &GUID_WICPixelFormat24bppBGR)) {
		hr = IWICImagingFactory_CreateFormatConverter(pFactory, &pConverter);
		if (FAILED(hr)) goto done;
		hr = IWICFormatConverter_Initialize(pConverter, pSource, &GUID_WICPixelFormat24bppBGR, WICBitmapDitherTypeNone, NULL, 0., WICBitmapPaletteTypeCustom);
		if (FAILED(hr)) goto done;
		pSource = (IWICBitmapSource *)pConverter;
	}

	/* Create a DIB and initialize it from the WIC source. */
	BITMAPINFO bmi = {{
		.biSize = sizeof(BITMAPINFOHEADER),
		.biWidth = (LONG)uiWidth,
		.biHeight = -(LONG)uiHeight,
		.biPlanes = 1,
		.biBitCount = 24,
		.biCompression = BI_RGB,
	}};
	UINT uiStride = (uiWidth * 3 + 3) & ~3; /* rounded up to a multiple of 4 */
	PVOID pvBits = NULL;
	hBitmap = CreateDIBSection(NULL, &bmi, DIB_RGB_COLORS, &pvBits, NULL, 0);
	if (!hBitmap) goto done;
	hr = IWICBitmapSource_CopyPixels(pSource, NULL, uiStride, uiStride * uiHeight, pvBits);
	if (FAILED(hr)) {
		DeleteObject(hBitmap);
		hBitmap = NULL;
	}

done:
	if (pScaler) IUnknown_Release(pScaler);
	if (pConverter) IUnknown_Release(pConverter);
	if (pFrame) IUnknown_Release(pFrame);
	if (pDecoder) IUnknown_Release(pDecoder);
	if (pFactory) IUnknown_Release(pFactory);
	CoUninitialize();
	return hBitmap;
}

/* Show the Splash Window
 *
 * Open the bitmap, insert into the splash window and display it.
 *
 */
int showSplash( const _TCHAR* featureImage )
{
	static int splashing = 0;
    HBITMAP hBitmap = 0;
    BITMAP  bmp;
    HDC     hDC;
    int     x, y;
    int     width, height;
    int     dpiX;

	if(splashing) {
		/*splash screen is already showing, do nothing */
		return 0;
	}
	if (featureImage == NULL)
		return -1;
	
	/* if Java was started first and is calling back to show the splash, we might not
	 * have initialized the window system yet
	 */
	initWindowSystem(0, NULL);
	
    /* fetch screen DPI and round it according to the swt.autoScale setting, 
    this implementation needs to be kept in sync with org.eclipse.swt.internal.DPIUtil#setDeviceZoom(int) */
    hDC = GetDC( NULL);
	dpiX = GetDeviceCaps ( hDC, LOGPIXELSX );
	ReleaseDC(NULL, hDC);
	switch (autoScaleValue) {
		case AUTOSCALE_FALSE:
			dpiX = 96;
			break;
		case AUTOSCALE_QUARTER:
			dpiX = ((dpiX + 12) / 24) * 24;
			break;
		case AUTOSCALE_EXACT:
			break;
		case AUTOSCALE_DEFAULT:
		case AUTOSCALE_INTEGER:
			dpiX = ((int)((dpiX + 24) / 96 )) * 96;
			if (autoScaleValue == AUTOSCALE_DEFAULT) {
				if (dpiX > 192) dpiX = 192;
			}
			break;
		default:
			dpiX = (96 * autoScaleValue + 50) / 100;
			break;
	}

	/* Load the bitmap for the feature. */
	if (featureImage != NULL)
		hBitmap = loadImage(featureImage, dpiX);

    /* If the bitmap could not be found, return an error. */
    if (hBitmap == 0)
    	return ERROR_FILE_NOT_FOUND;
    
	GetObject(hBitmap, sizeof(BITMAP), &bmp);

    /* figure out position */
    width = GetSystemMetrics (SM_CXSCREEN);
    height = GetSystemMetrics (SM_CYSCREEN);
    x = (width - bmp.bmWidth) / 2;
    y = (height - bmp.bmHeight) / 2;

	/* Centre the splash window and display it. */
    SetWindowPos (topWindow, 0, x, y, bmp.bmWidth, bmp.bmHeight, SWP_NOZORDER | SWP_NOSIZE | SWP_NOACTIVATE);
    SendMessage( topWindow, STM_SETIMAGE, IMAGE_BITMAP, (LPARAM) hBitmap );
    ShowWindow( topWindow, SW_SHOW );
    BringWindowToTop( topWindow );
	splashing = 1;
	
    /* Process messages */
	dispatchMessages();
	return 0;
}

void dispatchMessages() {
	MSG     msg;
	
	if(topWindow == 0)
		return;
	while (PeekMessage( &msg, NULL, 0, 0, PM_REMOVE))
   	{
		TranslateMessage( &msg );
		DispatchMessage( &msg );
	}
}

jlong getSplashHandle() {
	return (jlong)topWindow;
}

void takeDownSplash() {
	HWND window = NULL;
	if(topWindow != NULL) {
		if (mutex != NULL) {
			KillTimer(topWindow, findWindowTimerId);

			window = findSWTMessageWindow();
			if (window != NULL) {
				sendOpenFileMessage(window);
			}

			ReleaseMutex(mutex);
			CloseHandle(mutex);
			mutex = 0;
		}

		DestroyWindow(topWindow);
		dispatchMessages();
		topWindow = 0;
	}
}

/* Get the window system specific VM args */
_TCHAR** getArgVM( _TCHAR *vm )
{
	return argVM;
}

/* Local functions */

_TCHAR * findVMLibrary( _TCHAR* command ) {
	_TCHAR* lib = findLib(command);
	if( lib != NULL ) {
		adjustSearchPath(lib);
	}
	return lib;
}

void adjustSearchPath( _TCHAR* vmLib ){
	_TCHAR ** paths;
	_TCHAR* cwd = NULL;
	_TCHAR * path = NULL, *newPath = NULL;
	_TCHAR * c;
	int i, length;
	int needAdjust = 0, freePath = 0;
	
	paths = getVMLibrarySearchPath(vmLib);
	
	/* bug 325902 - add current working dir to the end of the search path */
	length = GetCurrentDirectory(0, NULL);
	cwd = malloc((length + 1)* sizeof(_TCHAR));
	GetCurrentDirectory(length, cwd);
	cwd[length - 1] = pathSeparator;
	cwd[length] = 0;
	
	/* first call to GetEnvironmentVariable tells us how big to make the buffer */
	length = GetEnvironmentVariable(_T_ECLIPSE("PATH"), path, 0);
	if (length > 0) {
		_TCHAR* current [] = { cwd, NULL };
		path = malloc(length * sizeof(_TCHAR));
		GetEnvironmentVariable(_T_ECLIPSE("PATH"), path, length);
		needAdjust = !containsPaths(path, paths) || !containsPaths(path, current);
		freePath = 1;
	} else {
		path = _T_ECLIPSE("");
		freePath = 0;
		needAdjust = 1;
	}
	
	if (needAdjust) {
		c = concatStrings(paths);
		newPath = malloc((_tcslen(c) + length + 1 + _tcslen(cwd) + 1) * sizeof(_TCHAR));
		_stprintf(newPath, _T_ECLIPSE("%s%s%c%s"), c, path, pathSeparator, cwd);
		SetEnvironmentVariable( _T_ECLIPSE("PATH"), newPath);
		free(c);
		free(newPath);
	}
	
	for (i = 0; paths[i] != NULL; i++)
		free(paths[i]);
	free(paths);
	free(cwd);
	if (freePath)
		free(path);
}
/*
 * Find the VM shared library starting from the java executable 
 */
static _TCHAR* findLib( _TCHAR* command ) {
	int i, j;
	size_t pathLength;	
	struct _stat stats;
	_TCHAR * path;				/* path to resulting jvm shared library */
	_TCHAR * location;			/* points to begining of jvmLocations section of path */
	
	/* for looking in the registry */
	HKEY jreKey = NULL;
	DWORD length = MAX_PATH;
	_TCHAR keyName[MAX_PATH];
	_TCHAR * jreKeyName;		
	
	if (command != NULL) {
		location = lastDirSeparator( command ) + 1;
		
		/*check first to see if command already points to the library */
		if (isVMLibrary(command)) {
			if (_tstat( command, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
				return command; 	/* exists */
			return NULL; /* doesn't exist */
		}
		
		pathLength = (size_t) (location - command);
		path = malloc((pathLength + MAX_LOCATION_LENGTH + 1 + _tcslen(vmLibrary) + 1) * sizeof(_TCHAR));
		_tcsncpy(path, command, pathLength);
		location = &path[pathLength];
		 
		/* 
		 * We are trying base/jvmLocations[*]/vmLibrary
		 * where base is the directory containing the given java command, normally jre/bin
		 */
		i = -1;
		while(jvmLocations[++i] != NULL) {
			_stprintf(location, _T_ECLIPSE("%s%c%s"), jvmLocations[i], dirSeparator, vmLibrary);
			if (_tstat( path, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
			{	/* found it */
				return path;
			}
		}
		
		/* if command is eclipse/jre, don't look in registry */
		 location = malloc( (_tcslen( getProgramDir() ) + _tcslen( shippedVMDir ) + 1) * sizeof(_TCHAR) );
        _stprintf( location, _T_ECLIPSE("%s%s"), getProgramDir(), shippedVMDir );
        if( _tcsncmp(command, location, _tcslen(location)) == 0) {
        	free(location);
        	return NULL;
        }
        free(location);
	}
	
	/* Not found yet, try the registry, we will use the first vm >= 1.6 */
	jreKeyName = _T("Software\\JavaSoft\\Java Runtime Environment");
	if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, jreKeyName, 0, KEY_READ, &jreKey) == ERROR_SUCCESS) {
		if(RegQueryValueEx(jreKey, _T_ECLIPSE("CurrentVersion"), NULL, NULL, (void*)&keyName, &length) == ERROR_SUCCESS) {
			path = checkVMRegistryKey(jreKey, keyName);
			if (path != NULL) {
				RegCloseKey(jreKey);
				return path;
			}
		}
		j = 0;
		length = MAX_PATH;
		while (RegEnumKeyEx(jreKey, j++, keyName, &length, 0, 0, 0, 0) == ERROR_SUCCESS) {  
			/*look for a 1.6+ vm*/
			if( _tcsncmp(_T("1.6"), keyName, 3) <= 0 ) {
				path = checkVMRegistryKey(jreKey, keyName);
				if (path != NULL) {
					RegCloseKey(jreKey);
					return path;
				}
			}
		}
		RegCloseKey(jreKey);
	}
	return NULL;
}

/*
 * Read the subKeyName subKey of jreKey and look to see if it has a Value 
 * "RuntimeLib" which points to a jvm library we can use 
 * 
 * Does not close jreKey
 */
static _TCHAR* checkVMRegistryKey(HKEY jreKey, _TCHAR* subKeyName) {
	_TCHAR value[MAX_PATH];
	HKEY subKey = NULL;
	DWORD length = MAX_PATH;
	_TCHAR *result = NULL;
	struct _stat stats;
	
	if(RegOpenKeyEx(jreKey, subKeyName, 0, KEY_READ, &subKey) == ERROR_SUCCESS) {				
		/*The RuntimeLib value should point to the library we want*/
		if(RegQueryValueEx(subKey, _T("RuntimeLib"), NULL, NULL, (void*)&value, &length) == ERROR_SUCCESS) {
			if (_tstat( value, &stats ) == 0 && (stats.st_mode & S_IFREG) != 0)
			{	/*library exists*/
				result = _tcsdup(value);
			}
		}
		RegCloseKey(subKey);
	}
	return result;
}

static _TCHAR* buildCommandLine( _TCHAR* program, _TCHAR* args[] )
{
	int   index, slash;
	size_t length = 0;
	_TCHAR *commandLine, *ch, *space;

	/*
	* Build the command line. Any argument with spaces must be in
	* double quotes in the command line. 
	*/
	if(program != NULL) 
		length = _tcslen(program) + 1;
	for (index = 0; args[index] != NULL; index++)
	{
		/* String length plus space character */
		length += _tcslen( args[ index ] ) + 1;
		/* Quotes + potential escaping '\' */
		if (_tcschr( args[ index ], _T(' ') ) != NULL) length += 3;
	}
	
	commandLine = ch = malloc ( (length + 1) * sizeof(_TCHAR) );
	if (program != NULL) {
		_tcscpy(ch, program);
		ch += _tcslen(program);
		*ch++ = _T(' ');
	}
	for (index = 0; args[index] != NULL; index++)
	{
		space = _tcschr( args[ index ], _T(' '));
		if (space != NULL) *ch++ = _T('\"');
		_tcscpy( ch, args[index] );
		ch += _tcslen( args[index] );
		if (space != NULL) {
			if ( *(ch - 1) == _T('\\') ) {
				/* escape a trailing unescaped '\' or it will escape our closing '"' and mess things up */
				slash = 1;
				while ( *(ch - 1 - slash) == _T('\\')) slash++;
				if (slash % 2) *ch++ = _T('\\');
			}
			*ch++ = _T('\"');
		}
		*ch++ = _T(' ');
	}
	*ch = _T('\0');
	return commandLine;
}
void restartLauncher( _TCHAR* program, _TCHAR* args[] )
{
	_TCHAR* commandLine = buildCommandLine(program, args);
	
	{
	STARTUPINFO    si;
    PROCESS_INFORMATION  pi;
    GetStartupInfo(&si);
    if (CreateProcess(NULL, commandLine, NULL, NULL, TRUE, 0, NULL, NULL, &si, &pi)) {
    	CloseHandle( pi.hThread );
    }   
	}
	free(commandLine);
}

JavaResults* launchJavaVM( _TCHAR* args[] )
{
	MSG msg;
	_TCHAR* commandLine;
	jvmProcess = 0;
	commandLine = buildCommandLine(NULL, args);
	jvmResults = malloc(sizeof(JavaResults));
	memset(jvmResults, 0, sizeof(JavaResults));
	
	/*
	* Start the Java virtual machine. Use CreateProcess() instead of spawnv()
	* otherwise the arguments cannot be freed since spawnv() segments fault.
	*/
	{
	STARTUPINFO    si;
    PROCESS_INFORMATION  pi;
    GetStartupInfo(&si);
    if (CreateProcess(NULL, commandLine, NULL, NULL, TRUE, 0, NULL, NULL, &si, &pi)) {
    	CloseHandle( pi.hThread );
    	jvmProcess = pi.hProcess;
    } else {
    	jvmResults->launchResult = errno;
    }
	}

	free( commandLine );

	if (jvmProcess > 0)
	{
        /* Set a timer to detect JVM process termination. */
        SetTimer( topWindow, jvmExitTimerId, jvmExitTimeout, detectJvmExit );

    	/* Process messages until the JVM terminates.
    	   This launcher process must continue to process events until the JVM exits
    	   or else Windows 2K will hang if the desktop properties (e.g., background) are
    	   changed by the user. Windows does a SendMessage() to every top level window
    	   process, which blocks the caller until the process responds. */
   		while (jvmProcess != 0)
   		{
   			GetMessage( &msg, NULL, 0, 0 );
			TranslateMessage( &msg );
			DispatchMessage( &msg );
		}

		/* Kill the timer. */
        KillTimer( topWindow, jvmExitTimerId );
	}

	/* Return the exit code from the JVM. */
	return jvmResults;
}

/* Detect JVM Process Termination */
static void CALLBACK detectJvmExit( HWND hwnd, UINT uMsg, UINT id, DWORD dwTime )
{
    DWORD   exitCode;

    /* If the JVM process has terminated */
    if (!GetExitCodeProcess( (HANDLE)jvmProcess, &exitCode ) ||
    		 exitCode != STILL_ACTIVE)
    {
    	/* Save the JVM exit code. This should cause the loop in launchJavaVM() to exit. */
        jvmResults->runResult = exitCode;
        jvmProcess = 0;
    }
}

void processVMArgs(_TCHAR **vmargs[] ) {
	_TCHAR** arg;
	for (arg = *vmargs; *arg != NULL; arg++) {
		if (_tcsncmp(*arg, _T("-Dswt.autoScale="), 16) == 0) {
			// see org.eclipse.swt.internal.DPIUtil.getZoomForAutoscaleProperty()
			_TCHAR* value = *arg + 16;
			if (_tcsicmp(value, _T("false")) == 0) {
				autoScaleValue = AUTOSCALE_FALSE;
			}
			else if (_tcsicmp(value, _T("quarter")) == 0) {
				autoScaleValue = AUTOSCALE_QUARTER;
			}
			else if (_tcsicmp(value, _T("exact")) == 0) {
				autoScaleValue = AUTOSCALE_EXACT;
			}
			else if (_tcsicmp(value, _T("integer")) == 0) {
				autoScaleValue = AUTOSCALE_INTEGER;
			}
			else {
				_TCHAR* end = NULL;
				autoScaleValue = (int)_tcstol(value, &end, 10);
				if (end == NULL || *end != _T('\0')) {
					/* conversion error, reset to default */
					autoScaleValue = AUTOSCALE_DEFAULT;
				}
				else {
					if (autoScaleValue < 25) autoScaleValue = 25;
					else if (autoScaleValue > 1600) autoScaleValue = 1600;
				}
			}
		}
	}
}

JavaResults* startJavaVM( _TCHAR* libPath, _TCHAR* vmArgs[], _TCHAR* progArgs[], _TCHAR* jarFile )
{
	return startJavaJNI(libPath, vmArgs, progArgs, jarFile);
}

/* returns 1 if the JVM version is >= 9, 0 otherwise */
int isModularVM( _TCHAR * javaVM, _TCHAR * jniLib ) {
	_TCHAR *vm = (jniLib != NULL) ? jniLib : javaVM;
	int result = 0;
	DWORD infoSize;
	DWORD handle;
	void * info;
	
	_TCHAR *versionKey, *version, *majorVersion = NULL;
	int versionSize;
	
	if (vm == NULL)
		return 0;
	
	infoSize = GetFileVersionInfoSize(vm, &handle);
	if (infoSize > 0) {
		info = malloc(infoSize);
		if (GetFileVersionInfo(vm, 0,  infoSize, info)) {
			TRANSLATIONS * translations;
			int translationsSize;
			VerQueryValue(info,  _T_ECLIPSE("\\VarFileInfo\\Translation"), (void *) &translations, &translationsSize);

			/* this size is only right because %04x is 4 characters */
			versionKey = malloc((_tcslen(PRODUCT_VERSION_KEY) + 1) * sizeof(_TCHAR));
				_stprintf(versionKey, PRODUCT_VERSION_KEY, translations[0].language, translations[0].codepage);
				VerQueryValue(info, versionKey, (void *)&version, &versionSize);
				if (versionSize >= 1) {
					majorVersion = _tcstok(version, ".-");
					if ((majorVersion != NULL) && (_tcstol(majorVersion, NULL, 10) >= 9)) {
						result = 1;
					}
				}
			free(versionKey);
		}
		free(info);
	}
	return result;
}
