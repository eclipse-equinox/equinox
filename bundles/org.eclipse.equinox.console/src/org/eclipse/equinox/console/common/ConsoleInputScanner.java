/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.completion.CompletionHandler;
import org.osgi.framework.BundleContext;

/**
 * This class performs the processing of the input special characters,
 * and updates respectively what is displayed in the output. It handles
 * escape sequences, delete, backspace, arrows, insert, home, end, pageup, pagedown, tab completion.
 */
public class ConsoleInputScanner extends Scanner {

	private static final byte TAB = 9;
    private boolean isCR = false;
    private boolean replace = false;
    private boolean isCompletionMode = false;
    // shows if command history should be saved - it is turned off in cases when passwords are to be entered
    private boolean isHistoryEnabled = true;

    private final HistoryHolder history;
    private final SimpleByteBuffer buffer;
    private CommandSession session;
    private BundleContext context;
    private Candidates candidates;
    private int originalCursorPos;

    public ConsoleInputScanner(ConsoleInputStream toShell, OutputStream toTelnet) {
        super(toShell, toTelnet);
        history = new HistoryHolder();
        buffer = new SimpleByteBuffer();
    }
    
    public void toggleHistoryEnabled(boolean isEnabled) {
    	isHistoryEnabled = isEnabled;
    }
    
    public void setSession(CommandSession session) {
    	this.session = session; 
    }

    public void setContext(BundleContext context) {
    	this.context = context;
    }
    
    public void scan(int b) throws IOException {
        b &= 0xFF;
        if (isCR) {
            isCR = false;
            if (b == LF) {
                return;
            }
        }
        
        if (b != TAB) {
    		if (isCompletionMode == true) {
    			isCompletionMode = false;
    			candidates = null;
    			originalCursorPos = 0;
    		}
    	}
        
        if (isEsc) {
            scanEsc(b);
        } else {
            if (b == getBackspace()) {
        		backSpace();
        	} else if(b == TAB) {
        		if (isCompletionMode == false) {
        			isCompletionMode = true;
        			processTab();
        		} else {
        			processNextTab();
        		}
        	} else if (b == CR) {
        		isCR = true;
        		processData();
        	} else if (b == LF) {
        		processData();
        	} else if (b == ESC) {
        		startEsc();
            } else if (b == getDel()) {
        		delete();
        	} else {
        		if (b >= SPACE && b < MAX_CHAR) {
        			newChar(b);
        		}
        	}
        }
    }

    private void delete() throws IOException {
        clearLine();
        buffer.delete();
        echoBuff();
        flush();
    }

    private void backSpace() throws IOException {
        clearLine();
        buffer.backSpace();
        echoBuff();
        flush();
    }

    protected void clearLine() throws IOException {
        int size = buffer.getSize();
        int pos = buffer.getPos();
        for (int i = size - pos; i < size; i++) {
            echo(BS);
        }
        for (int i = 0; i < size; i++) {
            echo(SPACE);
        }
        for (int i = 0; i < size; i++) {
            echo(BS);
        }
    }

    protected void echoBuff() throws IOException {
        byte[] data = buffer.copyCurrentData();
        for (byte b : data) {
            echo(b);
        }
        int pos = buffer.getPos();
        for (int i = data.length; i > pos; i--) {
            echo(BS);
        }
    }

    protected void newChar(int b) throws IOException {
        if (buffer.getPos() < buffer.getSize()) {
            if (replace) {
                buffer.replace(b);
            } else {
                buffer.insert(b);
            }
            clearLine();
            echoBuff();
            flush();
        } else {
            if (replace) {
                buffer.replace(b);
            } else {
                buffer.insert(b);
            }
        }
    }
    
    protected void processTab() throws IOException {
    	CompletionHandler completionHandler = new CompletionHandler(context, session);
    	Map<String, Integer> completionCandidates = completionHandler.getCandidates(buffer.copyCurrentData(), buffer.getPos());
    	
    	if (completionCandidates.size() == 1) {	
            completeSingleCandidate(completionCandidates);   
            isCompletionMode = false;
            return;
        }
    	printNewLine();
        if (completionCandidates.size() == 0) {
            printCompletionError();
            isCompletionMode = false;
        } else {
        	processCandidates(completionCandidates);
        }
        printNewLine();
        printPrompt();
    }
    
    protected void processCandidates(Map<String, Integer> completionCandidates) throws IOException{
    	Set<String> candidatesNamesSet = completionCandidates.keySet();
        String[] candidatesNames = (candidatesNamesSet.toArray(new String[0]));
    	originalCursorPos = buffer.getPos();
    	String[] candidateSuffixes = new String[candidatesNames.length];
        for (int i = 0; i < candidatesNames.length; i++) {
        	String candidateName = candidatesNames[i];
        	candidateSuffixes[i] = getCandidateSuffix(candidateName, completionCandidates.get(candidateName), originalCursorPos);
            for (byte symbol : candidateName.getBytes()) {
                echo(symbol);
            }
            printNewLine();
        }
        
        String commonPrefix = getCommonPrefix(candidateSuffixes);
        candidates = new Candidates(removeCommonPrefix(candidateSuffixes, commonPrefix));
        printString(commonPrefix, false);
        originalCursorPos = buffer.getPos();
    }
    
    protected void processNextTab() throws IOException {
    	if (candidates == null) {
    		return;
    	}
    	
    	while (originalCursorPos < buffer.getPos()) {
			backSpace();
    	}
    	
    	String candidate = candidates.getCurrent();
    	if(!candidate.equals("")) {
    		printString(candidate, true);
    	}
    }
    
    protected void printCandidate(String candidate, int startIndex, int completionIndex) throws IOException {
    	String suffix = getCandidateSuffix(candidate, startIndex, completionIndex);
    	if(suffix.equals("")) {
    		return;
    	}
        printString(suffix, true);
    }
    
    protected void printString(String st, boolean isEcho) throws IOException {
    	for (byte symbol : st.getBytes()) {
            buffer.insert(symbol);
            if (isEcho){
            	echo(symbol);
            }
        }
        flush();
    }
    
    protected String getCommonPrefix(String[] names) {
    	if (names.length == 0) {
    		return "";
    	}
    	
    	if (names.length == 1) {
    		return names[0];
    	}
    	
    	StringBuilder builder = new StringBuilder();
    	char[] name = names[0].toCharArray();
    	for(char c : name) {
    		String prefix = builder.append(c).toString();
    		for (int i = 1; i < names.length; i ++) {
    			if (!names[i].startsWith(prefix)) {
    				return prefix.substring(0, prefix.length() - 1);
    			}
    		}
    	}
    	
    	return builder.toString();
    }
    
    protected String[] removeCommonPrefix(String [] names, String commonPrefix){
    	ArrayList<String> result = new ArrayList<String>();
    	for (String name : names) {
    		String nameWithoutPrefix = name.substring(commonPrefix.length());
    		if (nameWithoutPrefix.length() > 0) {
    			result.add(nameWithoutPrefix);
    		}
    	}
    	result.add("");
    	return result.toArray(new String[0]);
    }
    
    protected String getCandidateSuffix(String candidate, int startIndex, int completionIndex) {
    	int partialLength = completionIndex - startIndex;
        if (partialLength >= candidate.length()) {
        	return "";
        }
        return candidate.substring(partialLength);
    }
    
    protected void completeSingleCandidate(Map<String, Integer> completionCandidates) throws IOException {
    	Set<String> keys = completionCandidates.keySet();
        String key = (keys.toArray(new String[0]))[0];
        int startIndex = completionCandidates.get(key);
        printCandidate(key, startIndex, buffer.getPos()); 
    }
    
    protected void printCompletionError() throws IOException {
    	byte[] curr = buffer.getCurrentData();
        if (isHistoryEnabled == true) {
        	history.add(curr);
        }
        
        String errorMessage = "No completion available";
        for (byte symbol : errorMessage.getBytes()) {
            echo(symbol);
        }
    }
    
    protected void printNewLine() throws IOException{
    	echo(CR);
        echo(LF);
        flush();
    }
    
    protected void printPrompt() throws IOException{
    	echo('o');
        echo('s');
        echo('g');
        echo('i');
        echo('>');
        echo(SPACE);
        echoBuff();
        flush();
    }

    private void processData() throws IOException {
//        buffer.add(CR);
        buffer.add(LF);
        echo(CR);
        echo(LF);
        flush();
        byte[] curr = buffer.getCurrentData();
        if (isHistoryEnabled == true) {
        	history.add(curr);
        }
        toShell.add(curr);
    }

    public void resetHistory() {
        history.reset();
    }

    protected void scanEsc(final int b) throws IOException {
        esc += (char) b;
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
        switch (key) {
            case UP:
                processUpArrow();
                break;
            case DOWN:
                processDownArrow();
                break;
            case RIGHT:
                processRightArrow();
                break;
            case LEFT:
                processLeftArrow();
                break;
            case HOME:
                processHome();
                break;
            case END:
                processEnd();
                break;
            case PGUP:
                processPgUp();
                break;
            case PGDN:
                processPgDn();
                break;
            case INS:
                processIns();
                break;
            case DEL:
                delete();
                break;
            default: //CENTER
                break;
        }
    }

    private static final byte[] INVERSE_ON = {ESC, '[', '7', 'm'};
    private static final byte[] INVERSE_OFF = {ESC, '[', '2', '7', 'm'};

    private void echo(byte[] data) throws IOException {
        for (byte b : data) {
            echo(b);
        }
    }

    private void processIns() throws IOException {
        replace = !replace;
        int b = buffer.getCurrentChar();
        echo(INVERSE_ON);
        echo(replace ? 'R' : 'I');
        flush();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            //do not care $JL-EXC$
        }
        echo(INVERSE_OFF);
        echo(BS);
        echo(b == -1 ? SPACE : b);
        echo(BS);
        flush();
    }

    private void processPgDn() throws IOException {
        byte[] last = history.last();
        if (last != null) {
            clearLine();
            buffer.set(last);
            echoBuff();
            flush();
        }
    }

    private void processPgUp() throws IOException {
        byte[] first = history.first();
        if (first != null) {
            clearLine();
            buffer.set(first);
            echoBuff();
            flush();
        }
    }

    private void processHome() throws IOException {
        int pos = buffer.resetPos();
        if (pos > 0) {
            for (int i = 0; i < pos; i++) {
                echo(BS);
            }
            flush();
        }
    }

    private void processEnd() throws IOException {
        int b;
        while ((b = buffer.goRight()) != -1) {
            echo(b);
        }
        flush();
    }

    private void processLeftArrow() throws IOException {
        if (buffer.goLeft()) {
            echo(BS);
            flush();
        }
    }

    private void processRightArrow() throws IOException {
        int b = buffer.goRight();
        if (b != -1) {
            echo(b);
            flush();
        }
    }

    private void processDownArrow() throws IOException {
        byte[] next = history.next();
        if (next != null) {
            clearLine();
            buffer.set(next);
            echoBuff();
            flush();
        }
    }

    private void processUpArrow() throws IOException {
        clearLine();
        byte[] prev = history.prev();
        buffer.set(prev);
        echoBuff();
        flush();
    }
    
    private static class Candidates {
    	private String[] candidates;
    	private int currentCandidateIndex = 0;
    	
    	public Candidates(String[] candidates) {
    		this.candidates = candidates.clone();
    	}
    	
    	public String getCurrent() {
    		if (currentCandidateIndex >= candidates.length) {
    			currentCandidateIndex = 0;
    		}
    		
    		return candidates[currentCandidateIndex++];
    	}
    }
}
