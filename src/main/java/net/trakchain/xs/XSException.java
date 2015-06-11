/*
 *  XS - XML Shorthand
 *  https://github.com/miguelpardal/xs
 *
 *  Copyright (c) 2015 Miguel L. Pardal
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the MIT License
 *  which accompanies this distribution, and is available at
 *  http://opensource.org/licenses/MIT
 */
package net.trakchain.xs;


/**
 *  XSException represents a XML Shorthand expansion fault.
 *  It can store a message, a cause, and the input line number (if available).
 *
 *  @author Miguel L. Pardal
 */
public class XSException extends Exception {

    private static final long serialVersionUID = 1L;

    private Integer lineNumber;

    public XSException() { }

    public XSException(String message) {
        super(message);
    }

    public XSException(String message, Integer lineNumber) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public XSException(Throwable cause) {
        super(cause);
    }

    public XSException(Throwable cause, Integer lineNumber) {
        super(cause);
        this.lineNumber = lineNumber;
    }

    public XSException(String message, Throwable cause) {
        super(message, cause);
    }

    public XSException(String message, Throwable cause, Integer lineNumber) {
        super(message, cause);
        this.lineNumber = lineNumber;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

}
