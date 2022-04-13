package org.example;

import java.io.Serializable;

public class ApplicationException extends RuntimeException implements Serializable {

    public ApplicationException(String message) {
        super(message);
    }
}
