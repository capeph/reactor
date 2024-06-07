/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Inherited
public @interface ReactorMessage {

    int id() default 0;
}
