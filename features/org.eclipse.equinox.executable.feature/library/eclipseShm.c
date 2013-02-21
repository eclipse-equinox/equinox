/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Silenio Quarti
 *******************************************************************************/

#include "eclipseOS.h"
#include "eclipseShm.h"

static _TCHAR* ECLIPSE_UNITIALIZED = _T_ECLIPSE("ECLIPSE_UNINITIALIZED");

#ifdef _WIN32

#include <stdio.h>

#ifdef __MINGW32__
#include <stdlib.h>
#endif

int createSharedData(_TCHAR** id, int size) {
	HANDLE mapHandle = CreateFileMapping(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE, 0, size, NULL);
	if (mapHandle == 0) return -1;
	if (id != NULL) {
		*id = malloc(18 * sizeof(_TCHAR));
#ifdef WIN64
		_stprintf(*id, _T_ECLIPSE("%lx_%lx"), GetCurrentProcessId(), (DWORDLONG) mapHandle);
#else
		_stprintf(*id, _T_ECLIPSE("%lx_%lx"), GetCurrentProcessId(), (DWORD) mapHandle);
#endif
	}
	/* set the shared data to "uninitialized" */
	setSharedData(*id, ECLIPSE_UNITIALIZED);
	return 0;
}

static int getShmID(const _TCHAR* id, LPDWORD processID, LPHANDLE handle) {
	if (id != NULL && _tcslen(id) > 0) {
		DWORD i1;
#ifdef WIN64
		DWORDLONG i2;
#else
		DWORD i2;
#endif
		if (_stscanf(id, _T_ECLIPSE("%lx_%lx"), &i1, &i2) != 2) return -1;
		*processID = (DWORD)i1;
		*handle = (HANDLE)i2;
		return 0;
	}
	return -1;
}

int destroySharedData(_TCHAR* id) {
	DWORD processID;
	HANDLE handle;
	if (getShmID(id, &processID, &handle) == -1) return -1;
	if (!CloseHandle(handle)) return -1;
	return 0;
}

int getSharedData(_TCHAR* id, _TCHAR** data) {
	_TCHAR *sharedData, *newData = NULL;
	DWORD processID;
	HANDLE handle, mapHandle = NULL, processHandle;
	if (getShmID(id, &processID, &handle) == -1) return -1;
	if (processID == GetCurrentProcessId()) {
		mapHandle = handle;
	} else {
		processHandle = OpenProcess(PROCESS_ALL_ACCESS, FALSE, processID);
		if (processHandle == NULL) return -1;
		DuplicateHandle(processHandle, handle, GetCurrentProcess(), &mapHandle, DUPLICATE_SAME_ACCESS, FALSE, DUPLICATE_SAME_ACCESS);
		CloseHandle(processHandle);
	}
	if (mapHandle == NULL) return -1;
	sharedData = MapViewOfFile(handle, FILE_MAP_WRITE, 0, 0, 0);
	if (sharedData == NULL) return -1;
	if (_tcscmp(sharedData, ECLIPSE_UNITIALIZED)== 0) return 0;
	if (data != NULL) {
		size_t length = (_tcslen(sharedData) + 1) * sizeof(_TCHAR);
		newData = malloc(length);
		memcpy(newData, sharedData, length);
	}
	if (!UnmapViewOfFile(sharedData)) {
		free(newData);
		return -1;
	}
	if (handle != mapHandle) {
		CloseHandle(mapHandle);
	}
	*data = newData;
	return 0;
}

int setSharedData(const _TCHAR* id, const _TCHAR* data) {
	_TCHAR* sharedData;
	DWORD processID;
	HANDLE handle, mapHandle = NULL, processHandle;
	if (getShmID(id, &processID, &handle) == -1) return -1;
	if (processID == GetCurrentProcessId()) {
		mapHandle = handle;
	} else {
		processHandle = OpenProcess(PROCESS_ALL_ACCESS, FALSE, processID);
		if (processHandle == NULL) return -1;
		DuplicateHandle(processHandle, handle, GetCurrentProcess(), &mapHandle, DUPLICATE_SAME_ACCESS, FALSE, DUPLICATE_SAME_ACCESS);
		CloseHandle(processHandle);
	}
	if (mapHandle == NULL) return -1;
	sharedData = MapViewOfFile(mapHandle, FILE_MAP_WRITE, 0, 0, 0);
	if (sharedData == NULL) return -1;
	if (data != NULL) {
		size_t length = (_tcslen(data) + 1) * sizeof(_TCHAR);
		memcpy(sharedData, data, length);
	} else {
		memset(sharedData, 0, sizeof(_TCHAR));
	}
	if (!UnmapViewOfFile(sharedData)) {
		return -1;
	}
	if (handle != mapHandle) {
		CloseHandle(mapHandle);
	}
	return 0;
}

#elif PHOTON

#include <fcntl.h>
#include <sys/mman.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

int createSharedData(char** id, int size) {
	int fd;
	char* location = "/tmp/eclipse_%x";
	char* name = malloc(strlen(location) + 9);
	sprintf(name, location, getpid());
	if ((fd = shm_open(name, O_RDWR | O_CREAT, 0666 )) == -1) return -1;
    if (ftruncate(fd, size) == -1 ) {
    	shm_unlink(name);
    	return -1;
    }
    close( fd );
	if (id != NULL) {
		*id = name;
	}
	return 0;
}

int destroySharedData(char* id) {
	return shm_unlink(id);
}

int getSharedData(char* id, char** data) {
	int fd, length, size;
	char *sharedData, *newData = NULL;
	if ((fd = shm_open(id, O_RDWR, 0666 )) == -1) return -1;
	size = lseek(fd, 0, SEEK_END);
    sharedData = mmap( 0, size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0 );
    if (sharedData != MAP_FAILED) {
		if (data != NULL) {
			length = strlen(sharedData) + 1;
			newData = malloc(length);
			memcpy(newData, sharedData, length);
		}
		munmap(sharedData, size);
	}
    close(fd);
	*data = newData;
	return newData == NULL ? -1 : 0;
}

int setSharedData(char* id, char* data) {
	int fd, length, size;
	char *sharedData;
	if ((fd = shm_open(id, O_RDWR, 0666 )) == -1) return -1;
	size = lseek(fd, 0, SEEK_END);
    sharedData = mmap( 0, size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0 );
    if (sharedData != MAP_FAILED) {
		if (data != NULL) {
			length = strlen(data) + 1;
			memcpy(sharedData, data, length);
		} else {
			memset(sharedData, 0, sizeof(char));
		}
		munmap(sharedData, size);
	}
    close(fd);
	return 0;
}

#else /* Unix like platforms */

#include <sys/shm.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>

int createSharedData(char** id, int size) {
	int shmid;
	key_t key = getpid();
	if ((shmid = shmget(key, size, IPC_CREAT | 0666)) < 0) {
		return -1;
	}
	if (id != NULL) {
		*id = malloc(9 * sizeof(char));
		sprintf(*id, "%x", shmid);
	}
	setSharedData(*id, ECLIPSE_UNITIALIZED);
	return 0;
}

static int getShmID(const char* id) {
	int shmid = -1;
	/* Determine the shared memory id. */
	if (id != NULL && strlen(id) > 0) {
		sscanf(id, "%x", &shmid);
	}
	return shmid;
}

int destroySharedData(char* id) {
	int shmid = getShmID(id);
	if (shmid == -1) return -1;
	return shmctl(shmid, IPC_RMID, NULL);
}

int getSharedData( char* id, char** data ) {
	char *sharedData, *newData = NULL;
	int length;
	int shmid = getShmID(id);
	if (shmid == -1) return -1;
 	sharedData = shmat(shmid, (void *)0, 0);
    if (sharedData == (char *)(-1)) return -1;
    if (_tcscmp(sharedData, ECLIPSE_UNITIALIZED) == 0) return 0;
    length = strlen(sharedData) + 1;
    newData = malloc(length);
    memcpy(newData, sharedData, length);
	if (shmdt(sharedData) != 0) {
		free(newData);
		return -1;
	}
	*data = newData;
	return 0;
}

int setSharedData(const char* id, const char* data) {
	char* sharedData;
	int length;
	int shmid = getShmID(id);
	if (shmid == -1) return -1;
 	sharedData = shmat(shmid, (void *)0, 0);
	if (sharedData == (char *)(-1)) return -1;
	if (data != NULL) {
		length = strlen(data) + 1;
		memcpy(sharedData, data, length);
	} else {
		memset(sharedData, 0, sizeof(char));
	}
	if (shmdt(sharedData) != 0) {
		return -1;
	}
	return 0;
}

#endif /* Unix like platforms */
