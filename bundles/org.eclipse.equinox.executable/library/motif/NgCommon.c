/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

#include <stdlib.h>
#include <string.h>
#include "NgCommon.h"

/* Non-zero = big-endian architecture */
static BYTE4 hostIsMSB = 0;

/* Store last error msg */
#define MAX_MSG_SIZE 100
char errorMsg[MAX_MSG_SIZE];

/* Library initialization */
void NgInit()
{
	BYTE4 result = (BYTE4) 'A';
	
	/* determine the byte ordering of the host machine */
	hostIsMSB = (BYTE4) (*((char *) &result) != 'A');
	
	errorMsg[0] = 0;
}

/**
 * Memory allocation routine
 */
void *NgMalloc (UBYTE4 size) 
{
	return malloc (size);
}

/**
 * Memory allocation routine
 */
void NgFree (void *memblock) 
{
	if (memblock != NULL)
		free (memblock);
}

void NgMemSet (void *dest, UBYTE1 c, BYTE4 count)
{
	memset (dest, c, count);
}

void NgMemCpy (void *dest, void *src, BYTE4 count)
{
	memcpy (dest, src, count);
}

/**
 * Error Reporting
 */

ng_err_t NgError (ng_err_t error_type, char* msg) {
	if (msg != NULL)
	{
		/* Store a copy of the last error msg - truncate if necessary */
		size_t size = strlen (msg);
		if (size >= MAX_MSG_SIZE) size = MAX_MSG_SIZE - 1;
		NgMemCpy (errorMsg, msg, size);
		errorMsg[size] = 0;
	}	
	return error_type;
}

const char *NgGetLastErrorMsg()
{
	return errorMsg;
}

/**
 * Stream manipulation routines
 */
ng_err_t NgStreamInit (ng_stream_t *stream, char *fullname)
{
	stream->file = fopen (fullname, "rb");
	stream->size = 0;
	stream->pos = 0;
	if (stream->file == NULL) return NgError (ERR_NG, "Can't open file");
	return ERR_OK;
}

void NgStreamClose (ng_stream_t *stream)
{
	if (stream->file != NULL)
	{
		fclose (stream->file);
		stream->file = NULL;
	}
	stream->size = -1;
}

char NgStreamEof (ng_stream_t *stream) 
{
	return stream->size == -1;
}

BYTE4 NgStreamGetPosition (ng_stream_t *stream)
{
	return stream->pos;
}

BYTE4 NgStreamSkip (ng_stream_t *stream, BYTE4 nbr)
{
	if (stream->size == -1) return 0;
	if (fseek (stream->file, nbr, SEEK_CUR))
	{
		NgStreamClose (stream);
		return 0;
	}
	stream->pos += nbr;
	return nbr;
}

BYTE4 NgStreamRead (ng_stream_t *stream, char *buffer, BYTE4 nbr)
{
	size_t cnt;
	if (stream->size == -1) return 0;
	cnt = fread (buffer, sizeof (char), nbr, stream->file);
	if (cnt != nbr)
	{
		NgStreamClose (stream);
		return 0;
	}
	stream->pos += nbr;
	return nbr;
}

BYTE1 NgIsMSB()
{
	return hostIsMSB != 0;
}

UBYTE2 SystemToLittleEndianUBYTE2 (UBYTE2 value)
{
	return hostIsMSB ? ((value&0xFF) << 8)|((value&0xFF00)>>8) : value;
}

UBYTE4 SystemToLittleEndianUBYTE4 (UBYTE4 value)
{
	return hostIsMSB ? ((value&0xFF000000L)>>24)|((value&0xFF0000L)>>8) | ((value&0xFF00L)<<8) | ((value&0xFFL)<<24) : value;
}

UBYTE2 SystemToBigEndianUBYTE2 (UBYTE2 value)
{
	return hostIsMSB ? value : ((value&0xFF) << 8)|((value&0xFF00)>>8);
}

UBYTE2 LittleEndianToSystemUBYTE2 (UBYTE2 value)
{
	return hostIsMSB ? ((value&0xFF) << 8)|((value&0xFF00)>>8) : value;
}

UBYTE4 LittleEndianToSystemUBYTE4 (UBYTE4 value)
{
	return hostIsMSB ? ((value&0xFF000000L)>>24)|((value&0xFF0000L)>>8) | ((value&0xFF00L)<<8) | ((value&0xFFL)<<24) : value;
}

UBYTE2 BigEndianToSystemUBYTE2 (UBYTE2 value)
{
	return hostIsMSB ? value : ((value&0xFF) << 8)|((value&0xFF00)>>8);
}

UBYTE4 BigEndianToSystemUBYTE4 (UBYTE4 value)
{
	return hostIsMSB ? value : ((value&0xFF000000L)>>24)|((value&0xFF0000L)>>8)|((value&0xFF00L)<<8) | ((value&0xFFL)<<24);
}
