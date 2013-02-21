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

#ifndef __NG_COMMON_H
#define __NG_COMMON_H

#include <memory.h>
#include <stdio.h>

typedef char BYTE1;
typedef unsigned char UBYTE1;
typedef short BYTE2;
typedef unsigned short UBYTE2;
typedef long BYTE4;
typedef unsigned long UBYTE4;

/* error reporting */
#define ERR_OK 1
#define ERR_SUBSCRIPT_OUT_OF_RANGE -1
#define ERR_INVALID_BIT_COUNT -2
#define ERR_NG -4

typedef BYTE4 ng_err_t;
ng_err_t NgError (ng_err_t error_type, char* msg);
const char *NgGetLastErrorMsg();

/**
 * NgInit
 * Must be called prior to using the image decoders 
 */
void NgInit();

/* memory management */
void *NgMalloc (UBYTE4 size);
void NgFree (void *memblock);
void NgMemSet (void *dest, UBYTE1 c, BYTE4 count);
void NgMemCpy (void *dest, void *src, BYTE4 count);

/* stream api */
typedef struct {
	FILE *file;
	BYTE4 size;
	BYTE4 pos;
} ng_stream_t;

/**
 * Init a stream given the path and name of a file
 * Note.  NgStreamClose should be called to release
 * the related OS resource.
 */
ng_err_t NgStreamInit (ng_stream_t *stream, char *fullname);

/**
 * Close any OS resource managed the given stream.
 * In particular, close the file if the stream is using one.
 */
void NgStreamClose (ng_stream_t *stream);

char NgStreamEof (ng_stream_t *stream);

BYTE4 NgStreamGetPosition (ng_stream_t *stream);
/**
 * Skips nbr bytes.
 * Return nbr if all bytes were skipped.
 * If nbr bytes can't be skipped, the stream is closed
 * (NgStreamEof returns 1). 0 is returned.
 */
BYTE4 NgStreamSkip (ng_stream_t *stream, BYTE4 nbr);
/**
 * Copies nbr bytes to buffer from stream.
 * Returns nbr if all bytes were copied.
 * If nbr bytes can't be read, no bytes are copied. The stream
 * is closed (NgStreamEof returns 1). 0 is returned.
 */
BYTE4 NgStreamRead (ng_stream_t *stream, char *buffer, BYTE4 nbr);

/* little/big endian conversion */
BYTE1 NgIsMSB();
UBYTE2 SystemToLittleEndianUBYTE2 (UBYTE2);
UBYTE4 SystemToLittleEndianUBYTE4 (UBYTE4);
UBYTE2 SystemToBigEndianUBYTE2 (UBYTE2);
UBYTE2 LittleEndianToSystemUBYTE2 (UBYTE2);
UBYTE4 LittleEndianToSystemUBYTE4 (UBYTE4);
UBYTE2 BigEndianToSystemUBYTE2 (UBYTE2);
UBYTE4 BigEndianToSystemUBYTE4 (UBYTE4);

#endif /* NG_COMMON_H */
