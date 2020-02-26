/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.telemetry.compiler;


import com.google.auto.service.AutoService;

import org.mozilla.telemetry.annotation.TelemetryDoc;
import org.mozilla.telemetry.annotation.TelemetryExtra;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@AutoService(Processor.class)
public class TelemetryAnnotationProcessor extends AbstractProcessor {

    static final String fileReadme = "/docs/events.md";
    private static final String fileAmplitudeMapping = "/docs/view.sql";
    private static final String FILE_SOURCE_SQL = "view-replace.sql";
    private static final String FILE_SOURCE_SQL_PLACE_HOLDER = "---REPLACE---ME---";


    // TODO: TelemetryEvent's fields are private, I'll create a PR to make them public so I can
    // test the ping format in compile time.
    static class TelemetryEventConstant {
        private static final int MAX_LENGTH_CATEGORY = 30;
        private static final int MAX_LENGTH_METHOD = 20;
        private static final int MAX_LENGTH_OBJECT = 20;
        private static final int MAX_LENGTH_VALUE = 80;
        private static final int MAX_EXTRA_KEYS = 200;
        private static final int MAX_LENGTH_EXTRA_KEY = 15;
        private static final int MAX_LENGTH_EXTRA_VALUE = 80;
    }


    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(TelemetryDoc.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Collection<? extends Element> annotatedElements =
                env.getElementsAnnotatedWith(TelemetryDoc.class);

        final String projectRootDir = processingEnv.getOptions().get("projectRootDir");

        if (annotatedElements.size() == 0) {
            return false;
        }
        try {

            final String header = "| Event | category | method | object | value | extra |\n" +
                    "| ---- | ---- | ---- | ---- | ---- | ---- |\n";
            genDoc(annotatedElements, header, projectRootDir + fileReadme, '|');

            genSQL(annotatedElements, projectRootDir + fileAmplitudeMapping);


        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Exception while creating Telemetry related documents" + e);
            e.printStackTrace();
        }


        return false;
    }


    private void genDoc(Collection<? extends Element> annotatedElements, String header, String path, char separator) throws FileNotFoundException {

        final File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        File directory = new File(file.getParentFile().getAbsolutePath());
        directory.mkdirs();


        final PrintWriter printWriter = new PrintWriter(new FileOutputStream(file, true));
        StringBuffer sb = new StringBuffer().append(header);

        char start = separator;
        char end = ' ';

        // check duplication
        final HashMap<String, Boolean> lookup = new HashMap<>();

        for (Element type : annotatedElements) {
            if (type.getKind() == ElementKind.METHOD) {
                final TelemetryDoc annotation = type.getAnnotation(TelemetryDoc.class);
                verifyEventFormat(annotation);
                final String result = verifyEventDuplication(annotation, lookup);
                if (result != null) {
                    throw new IllegalArgumentException("Duplicate event combination:" + annotation + "\n" + result);
                }

                // value may have ',' so we add a placeholder '"' for csv files
                sb.append(start).append(annotation.name()).append(separator)
                        .append(annotation.category()).append(separator)
                        .append(annotation.method()).append(separator)
                        .append(annotation.object()).append(separator)
                        .append('"').append(annotation.value()).append('"').append(separator);

                // extras may have ',' so we add a placeholder '"' for csv files
                sb.append('"');
                for (TelemetryExtra extra : annotation.extras()) {
                    sb.append(extra.name()).append("=").append(extra.value() + ',');
                }
                sb.append('"');
                sb.append(end);
                printWriter.println(sb);
                sb = new StringBuffer();
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "This should not happen:" + type);
            }
        }


        printWriter.close();
    }

    private void genSQL(Collection<? extends Element> annotatedElements, String path) throws IOException {

        final File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        File directory = new File(file.getParentFile().getAbsolutePath());
        directory.mkdirs();


        final PrintWriter printWriter = new PrintWriter(new FileOutputStream(file, true));
        StringBuffer sb = new StringBuffer();


        // check duplication
        final HashMap<String, Boolean> lookup = new HashMap<>();

        for (Element type : annotatedElements) {
            if (type.getKind() == ElementKind.METHOD) {
                final TelemetryDoc annotation = type.getAnnotation(TelemetryDoc.class);
                if (annotation.skipAmplitude()) {
                    continue;
                }
                verifyAmplitudeMappingFormat(annotation);
                final String result = verifyEventDuplication(annotation, lookup);
                if (result != null) {
                    throw new IllegalArgumentException("Duplicate event combination:" + annotation + "\n" + result);
                }
                StringBuilder partValue = new StringBuilder();
                String telemetryValue = annotation.value();
                if (!telemetryValue.isEmpty()) {
                    partValue.append("AND (event_value IN (");
                    ArrayList<String> split = new ArrayList<>(Arrays.asList(telemetryValue.split(",")));
                    boolean hasNull = false;
                    for (String value : split) {
                        if (value.equals("null")) {
                            hasNull = true;
                            continue;
                        }
                        partValue.append("'").append(value).append("', ");
                    }
                    partValue.deleteCharAt(partValue.length() - 1).deleteCharAt(partValue.length() - 1);
                    if (hasNull) {
                        partValue.append(") OR event_value IS NULL) ");
                    } else {
                        partValue.append(") ) ");
                    }
                } else {
                    partValue.append("AND event_value IS NULL ");

                }

                String event = "        WHEN (event_category IN ('" + annotation.category() + "') ) AND (event_method IN ('" + annotation.method() +
                        "') ) AND (event_object IN ('" + annotation.object() + "') ) " + partValue.toString() + "THEN 'Rocket -  " + annotation.name().replace("'", "\\'") + "' ";
                sb.append(event);
                sb.append("\n");

            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "This should not happen:" + type);
            }
        }


        FileObject fileObject = processingEnv.getFiler().getResource(StandardLocation.ANNOTATION_PROCESSOR_PATH, "", FILE_SOURCE_SQL);
        InputStream inputStream = fileObject.openInputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(bufferedInputStream, StandardCharsets.UTF_8));

        String str;
        while ((str = bufferedReader.readLine()) != null) {
            str = str.replace(FILE_SOURCE_SQL_PLACE_HOLDER, sb.toString());
            printWriter.println(str);
        }
        bufferedInputStream.close();
        inputStream.close();

        printWriter.close();
    }

    private void verifyAmplitudeMappingFormat(TelemetryDoc annotation) {
        String pattern = "[A-Za-z0-9,_]*";
        if (!annotation.object().matches(pattern)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Contain invalid chars in Telemetry object:" + annotation.toString());
        }
        if (!annotation.method().matches(pattern)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Contain invalid chars in Telemetry method:" + annotation.toString());
        }
        if (!annotation.value().matches(pattern)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Contain invalid chars in Telemetry value:" + annotation.toString());
        }
    }

    String verifyEventDuplication(TelemetryDoc annotation, HashMap<String, Boolean> lookup) {
        StringBuilder key = new StringBuilder(annotation.category() + annotation.method() + annotation.object() + annotation.value());
        for (TelemetryExtra extra : annotation.extras()) {
            key.append(extra.name());
        }
        if (lookup.containsKey(key.toString())) {
            return key.toString();
        }
        lookup.put(key.toString(), true);
        return null;

    }

    private void verifyEventFormat(TelemetryDoc annotation) {
        final String action = annotation.category();
        if (action.length() > TelemetryEventConstant.MAX_LENGTH_CATEGORY) {
            throw new IllegalArgumentException("The length of category is too long:" + action);
        }
        final String method = annotation.method();
        if (method.length() > TelemetryEventConstant.MAX_LENGTH_METHOD) {
            throw new IllegalArgumentException("The length of method is too long:" + method);
        }
        final String object = annotation.object();
        if (object.length() > TelemetryEventConstant.MAX_LENGTH_OBJECT) {
            throw new IllegalArgumentException("The length of object is too long:" + object);
        }
        final String value = annotation.value();
        if (value.length() > TelemetryEventConstant.MAX_LENGTH_VALUE) {
            throw new IllegalArgumentException("The length of value is too long:" + value);
        }
        final TelemetryExtra[] extras = annotation.extras();
        if (extras.length > TelemetryEventConstant.MAX_EXTRA_KEYS) {
            throw new IllegalArgumentException("Too many extras");
        }
        for (TelemetryExtra extra : extras) {
            final String eName = extra.name();
            final String eVal = extra.value();
            if (eName.length() > TelemetryEventConstant.MAX_LENGTH_EXTRA_KEY) {
                throw new IllegalArgumentException("The length of extra key is too long:" + eName);
            }
            if (eVal.length() > TelemetryEventConstant.MAX_LENGTH_VALUE) {
                throw new IllegalArgumentException("The length of extra value is too long:" + eVal);
            }
        }

    }
}
