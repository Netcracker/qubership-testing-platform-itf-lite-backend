package org.qubership.atp.itf.lite.backend.conventions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.atp.itf.lite.backend.annotations.SerializableCheckable;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BasicAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BearerAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.InheritFromParentAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth1AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.PermissionEntity;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.auth.GetAuthorizationCode;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.javers.history.JvCommitPropertyId;
import org.qubership.atp.itf.lite.backend.model.entities.key.RequestSnapshotKey;
import org.reflections.Reflections;

public class ModelCheckSerializedTest {

    private static final String MODELS_PACKAGE = "org.qubership.atp.itf.lite.backend.model";

    @Test
    public void testModelCheckSerialized() {
        Set<Class<?>> annotatedModelClasses = annotatedModelClasses();
        Set<Class<? extends Serializable>> serializableClassesAfterExclude = getSerializableClassesWithExclude();

        Assertions.assertEquals(annotatedModelClasses.size(),
                serializableClassesAfterExclude.size(),
                "Collection sizes do not match. add an annotation or increment the test counter");
    }

    private void exclude(Set<Class<? extends Serializable>> classes) {
        List<Class> classesToExclude = new ArrayList<>();
        Collections.addAll(classesToExclude,
                InheritFromParentAuthorizationSaveRequest.class,
                OAuth1AuthorizationSaveRequest.class,
                OAuth2AuthorizationSaveRequest.class,
                BasicAuthorizationSaveRequest.class,
                HttpHeaderSaveRequest.class,
                HttpRequestEntitySaveRequest.class,
                BearerAuthorizationSaveRequest.class,
                RequestSnapshotKey.class,
                FormDataPart.class,
                PermissionEntity.class,
                JvCommitPropertyId.class,
                RequestBody.class,
                GetAuthorizationCode.class
        );
        classes.removeAll(classesToExclude);
    }

    private Set<Class<? extends Serializable>> getSerializableClassesWithExclude() {
        Reflections reflections = new Reflections(MODELS_PACKAGE);
        Set<Class<? extends Serializable>> classes = reflections.getSubTypesOf(Serializable.class);
        exclude(classes);
        return classes;
    }

    private Set<Class<?>> annotatedModelClasses() {
        Reflections annot = new Reflections(MODELS_PACKAGE);
        Set<Class<?>> annotatedModelClasses = annot
                .getTypesAnnotatedWith(SerializableCheckable.class, true);
        return annotatedModelClasses;
    }
}
