package org.gscript.data.library;

import java.lang.annotation.*;

import org.gscript.view.LibraryPropertiesView;

@Retention(RetentionPolicy.RUNTIME)
public @interface LibraryAttribute {
    String title();
    String description();
    int version();
    Class<? extends LibraryPropertiesView> view();
}