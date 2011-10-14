/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation  
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.console.telnet;

import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.common.ConsoleOutputStream;
import org.eclipse.equinox.console.common.KEYS;
import org.eclipse.equinox.console.common.Scanner;
import org.eclipse.equinox.console.common.terminal.TerminalTypeMappings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * This class performs the processing of the telnet commands,
 * and updates respectively what is displayed in the output. Also, it performs 
 * terminal type negotiation with the telnet client. This is important for some of the escape sequences, 
 * which are different for the different terminal types. Without such negotiation,
 * some keys (such as backspace, del, insert, home, etc.) may not be correctly
 * interpreted by the telnet server. Currently the supported terminal types are
 * ANSI, VT100, VT220, VT320, XTERM and SCO. The support is limited to the following 
 * keys - BACKSPACE, DEL, INSERT, HOME, END, PAGEUP, PAGEDOWN, ARROWS.
 */
public class TelnetInputScanner extends Scanner {

    private boolean isCommand = false;
    private boolean isReadingTtype = false; 
    private boolean shouldFinish = false;
    private boolean tTypeNegotiationStarted = false;
    private int lastRead = -1;
    private ArrayList<Integer> currentTerminalType = new ArrayList<Integer>();
    private ArrayList<Integer> lastTerminalType = null;
    private Set<String> supportedTerminalTypes = new HashSet<String>();
    private Callback callback;

    public TelnetInputScanner(ConsoleInputStream toShell, ConsoleOutputStream toTelnet, Callback callback) {
        super(toShell, toTelnet);
        initializeSupportedTerminalTypes();
        TerminalTypeMappings currentMapping = supportedEscapeSequences.get(DEFAULT_TTYPE);
    	currentEscapesToKey = currentMapping.getEscapesToKey();
    	escapes = currentMapping.getEscapes();
    	setBackspace(currentMapping.getBackspace());
    	setDel(currentMapping.getDel());
    	this.callback = callback;
    }
    
    private void initializeSupportedTerminalTypes() {
        supportedTerminalTypes.add("ANSI");
        supportedTerminalTypes.add("VT100");
        supportedTerminalTypes.add("VT220");
        supportedTerminalTypes.add("VT320");
        supportedTerminalTypes.add("XTERM");
        supportedTerminalTypes.add("SCO");     
    }

    public void scan(int b) throws IOException {
        b &= 0xFF;

        if (isEsc) {
            scanEsc(b);
        } else if (isCommand) {
            scanCommand(b);
        } else if (b == IAC) {
            startCommand();
        } else {
        	switch (b) {
                case ESC:
                    startEsc();
                    toShell.add(new byte[]{(byte) b});
                    break;
                default:
                    if (b >= SPACE && b < MAX_CHAR) {
                        echo((byte) b);
                        flush();
                    }
                    toShell.add(new byte[]{(byte) b});
            }

        }
        lastRead = b;
    }

    /* Telnet command codes are described in RFC 854, TELNET PROTOCOL SPECIFICATION
     * available at http://www.ietf.org/rfc/rfc854.txt
     * 
     * Telnet terminal type negotiation option is described in RFC 1091, Telnet Terminal-Type Option
     * available at http://www.ietf.org/rfc/rfc1091.txt
     */
    private static final int SE = 240;
    private static final int EC = 247;
    private static final int EL = 248;
    private static final int SB = 250;
    private static final int WILL = 251;
    private static final int WILL_NOT = 252;
    private static final int DO = 253;
    private static final int DO_NOT = 254;
    private static final int TTYPE = 24;
    private static final int SEND = 1;
    private static final int IAC = 255;
    private static final int IS = 0;

    private boolean isNegotiation;
    private boolean isWill;
    
    private byte[] tTypeRequest = {(byte)IAC, (byte)SB, (byte)TTYPE, (byte)SEND, (byte)IAC, (byte)SE};

    private void scanCommand(final int b) throws IOException {
        if (isNegotiation) {
            scanNegotiation(b);
        } else if (isWill) {
            isWill = false;
            isCommand = false;
            if(b == TTYPE && tTypeNegotiationStarted == false) {
        		sendRequest();
        	}
        } else {
            switch (b) {
                case WILL:
                	isWill = true;
                	break;
                case WILL_NOT:
                	break;
                case DO:
                	break;
                case DO_NOT:
                    break;
                case SB:
                    isNegotiation = true;
                    break;
                case EC:
                    eraseChar();
                    isCommand = false;
                    break;
                case EL:
                default:
                    isCommand = false;
                    break;
            }
        }
    }

    private void scanNegotiation(final int b) {
    	if (lastRead == SB && b == TTYPE) {
    		isReadingTtype = true;
    	} else if (b == IS) {
    		
    	} else if (b == IAC) {
    		
    	} else if (b == SE) {
            isNegotiation = false;
            isCommand = false;
            if (isReadingTtype == true) {
				isReadingTtype = false;
				if (shouldFinish == true) {
					setCurrentTerminalType();
					shouldFinish = false;
					return;
				}
				boolean isMatch = isTerminalTypeSupported();
				boolean isLast = isLast();
				if (isMatch == true) {
					setCurrentTerminalType();
					return;
				}
				lastTerminalType = currentTerminalType;
				currentTerminalType = new ArrayList<Integer>();
				if (isLast == true && isMatch == false) {
					shouldFinish = true;
					sendRequest();
				} else if (isLast == false && isMatch == false) {
					sendRequest();
				}
			}
        } else if (isReadingTtype == true){
        	currentTerminalType.add(b);
        }
    }
    
    private boolean isTerminalTypeSupported() {
    	byte[] tmp = new byte[currentTerminalType.size()];
    	int idx = 0;
    	for(Integer i : currentTerminalType) {
    		tmp[idx] = i.byteValue();
    		idx++;
    	}
    	String tType = new String(tmp);
    	
    	for(String terminal : supportedTerminalTypes) {
    		if(tType.toUpperCase().contains(terminal)) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    private boolean isLast() {
    	if(currentTerminalType.equals(lastTerminalType)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    private void setCurrentTerminalType() {
    	byte[] tmp = new byte[currentTerminalType.size()];
    	int idx = 0;
    	for(Integer i : currentTerminalType) {
    		tmp[idx] = i.byteValue();
    		idx++;
    	}
    	String tType = new String(tmp);
    	String term = null;
    	for(String terminal : supportedTerminalTypes) {
    		if(tType.toUpperCase().contains(terminal)) {
    			term = terminal;
    		}
    	}
    	TerminalTypeMappings currentMapping = supportedEscapeSequences.get(term);
    	if(currentMapping == null) {
    		currentMapping = supportedEscapeSequences.get(DEFAULT_TTYPE);
    	}
    	currentEscapesToKey = currentMapping.getEscapesToKey();
    	escapes = currentMapping.getEscapes();
    	setBackspace(currentMapping.getBackspace());
    	setDel(currentMapping.getDel());
    	if(callback != null) {
    		callback.finished();
    	}
    }
    
    private void sendRequest() {
    	try {
			toTelnet.write(tTypeRequest);
			toTelnet.flush();
			if(tTypeNegotiationStarted == false) {
				tTypeNegotiationStarted = true;
			}
		} catch (IOException e) {
			
			e.printStackTrace();
		}
    }

    private void startCommand() {
        isCommand = true;
        isNegotiation = false;
        isWill = false;
    }

    private void eraseChar() throws IOException {
        toShell.add(new byte[]{BS});
    }

    protected void scanEsc(int b) throws IOException {
        esc += (char) b;
        toShell.add(new byte[]{(byte) b});
        KEYS key = checkEscape(esc);
        if (key == KEYS.UNFINISHED) {
            return;
        }
        if (key == KEYS.UNKNOWN) {
            isEsc = false;
            scan(b);
            return;
        }
        isEsc = false;
    }

}
