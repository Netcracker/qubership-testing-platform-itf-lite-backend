package org.qubership.atp.itf.lite.backend.conventions;

import static com.google.common.collect.Sets.newHashSet;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.atp.common.probes.controllers.DeploymentController;
import org.qubership.atp.itf.lite.backend.controllers.CacheEvictController;
import org.qubership.atp.itf.lite.backend.controllers.PingController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.reflect.ClassPath;
import joptsimple.internal.Strings;
import lombok.SneakyThrows;

public class PreAuthorizeAnnotationConsistencyTest {

    private static final String CONTROLLERS_DIR = "org.qubership.atp.itf.lite.backend.controllers";
    private static final Pattern PRE_AUTHORIZE_PARAM_PATTERN = Pattern.compile("#[a-zA-z]+");
    private static final Set<Class<?>> CONTROLLER_EXCLUSIONS = newHashSet(
            CacheEvictController.class, PingController.class, DeploymentController.class);

    @Test
    public void verifyPreAuthorizeAnnotationExistence() {
        Set<Class<?>> allClasses = findAllClassesInPackage(CONTROLLERS_DIR);

        List<String> endpointViolations = new ArrayList<>();
        allClasses.stream()
                .filter(clazz -> !CONTROLLER_EXCLUSIONS.contains(clazz))
                .forEach(controllerClass -> {
                    Method[] controllerMethods = controllerClass.getDeclaredMethods();

                    List<String> currentEndpointViolations = Arrays.stream(controllerMethods)
                            .filter(this::isRestMethod)
                            .filter(method -> !method.isAnnotationPresent(PreAuthorize.class))
                            .map(method -> controllerClass.getName() + "#" + method.getName() + "()")
                            .collect(Collectors.toList());

                    endpointViolations.addAll(currentEndpointViolations);
                });

        String violationMethodPaths = String.join("\n", endpointViolations);
        String errMsg = "@PreAuthorize is missed at next REST endpoint(s): \n\n" + violationMethodPaths;

        Assertions.assertTrue(endpointViolations.isEmpty(), errMsg);
    }

    @Test
    public void verifyPreAuthorizeAnnotationParametersConsistency() {
        Set<Class<?>> allClasses = findAllClassesInPackage(CONTROLLERS_DIR);

        allClasses.stream()
                .filter(clazz -> !CONTROLLER_EXCLUSIONS.contains(clazz))
                .forEach(controllerClass -> {
                    Method[] controllerMethods = controllerClass.getDeclaredMethods();

                    Arrays.stream(controllerMethods)
                            .filter(this::isRestMethod)
                            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                            .forEach(method -> {
                                String preAuthorizeValue = method.getAnnotation(PreAuthorize.class).value();

                                Matcher matcher = PRE_AUTHORIZE_PARAM_PATTERN.matcher(preAuthorizeValue);

                                Set<String> methodParameterNames = Arrays.stream(method.getParameters())
                                        .map(Parameter::getName)
                                        .collect(Collectors.toSet());

                                while (matcher.find()) {
                                    String param = matcher.group(0).replaceAll("#", Strings.EMPTY);
                                    String endpointPath = controllerClass.getName() + "#" + method.getName() + "()";

                                    String errMsg = String.format("@PreAuthorize annotation at %s endpoint contains "
                                                    + "param #%s which is not declared in existed method params: %s",
                                            endpointPath, param, methodParameterNames);

                                    Assertions.assertTrue(methodParameterNames.contains(param), errMsg);
                                }
                            });
                });
    }

    @SneakyThrows
    private Set<Class<?>> findAllClassesInPackage(String packageName) {
        return ClassPath.from(this.getClass().getClassLoader())
                .getAllClasses()
                .stream()
                .filter(clazz -> clazz.getPackageName().equalsIgnoreCase(packageName))
                .map(clazz -> clazz.load())
                .collect(Collectors.toSet());
    }

    private boolean isRestMethod(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PatchMapping.class);
    }
}
