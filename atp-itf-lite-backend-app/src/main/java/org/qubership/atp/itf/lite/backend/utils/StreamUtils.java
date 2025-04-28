/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.itf.lite.backend.utils;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractEntity;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractNamedEntity;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class StreamUtils {

    private static final ModelMapper modelMapper = new ModelMapper();

    /**
     * Extract id's from any type entities.
     *
     * @param entities  input entities
     * @param extractor id extractor
     * @param <T>       processed entities type
     * @return result set
     */
    public static <T> Set<UUID> extractIds(Collection<T> entities, Function<T, UUID> extractor) {
        if (entities == null) {
            return Collections.emptySet();
        }

        return getIdsStream(entities, extractor)
                .collect(toSet());
    }

    /**
     * Extract id's set from any type entities.
     *
     * @param entities input entities
     * @param <T>      processed entities type
     * @return result set
     */
    public static <T extends AbstractEntity> Set<UUID> extractIds(Collection<T> entities) {
        return extractIds(entities, AbstractEntity::getId);
    }

    /**
     * Extract flat id's from any type entities.
     *
     * @param entities  input entities
     * @param extractor id extractor
     * @param <T>       processed entities type
     * @return result set
     */
    public static <T> Set<UUID> extractFlatIds(Collection<T> entities, Function<T, Collection<UUID>> extractor) {
        if (entities == null) {
            return Collections.emptySet();
        }

        return getFlatIdsStream(entities, extractor)
                .collect(toSet());
    }

    /**
     * Get id's stream from any type entities.
     *
     * @param entities  input entities
     * @param extractor id extractor
     * @param <T>       processed entities type
     * @return result stream
     */
    private static <T> Stream<UUID> getIdsStream(Collection<T> entities, Function<T, UUID> extractor) {
        return entities.stream()
                .map(extractor)
                .filter(Objects::nonNull);
    }

    private static <T> Stream<UUID> getFlatIdsStream(Collection<T> entities, Function<T, Collection<UUID>> extractor) {
        return entities.stream()
                .filter(elem -> !isEmpty(extractor.apply(elem)))
                .flatMap(elem -> extractor.apply(elem).stream());
    }

    private static <T> Stream<T> stream(Iterable<T> entities) {
        return StreamSupport.stream(entities.spliterator(), false);
    }

    public static <T> Map<UUID, T> toIdEntityMap(Iterable<T> entities, Function<T, UUID> keyExtractor) {
        return stream(entities)
                .collect(Collectors.toMap(keyExtractor, identity()));
    }

    public static <T extends AbstractNamedEntity> Map<UUID, T> toIdEntityMap(Iterable<T> entities) {
        return stream(entities)
                .collect(Collectors.toMap(AbstractNamedEntity::getId, identity()));
    }

    public static <T, R> Map<R, T> toEntityMap(Iterable<T> entities, Function<T, R> keyExtractor) {
        return stream(entities)
                .collect(Collectors.toMap(keyExtractor, identity()));
    }

    public static <T, R> Map<R, T> toEntityMapWithMergeFunction(Iterable<T> entities, Function<T, R> keyExtractor,
                                                                   BinaryOperator<T> mergeFunction) {
        return stream(entities)
                .collect(Collectors.toMap(keyExtractor, identity(), mergeFunction));
    }

    public static <T, K, V> Map<K, V> toMap(Iterable<T> entities, Function<T, K> keyExtractor,
                                            Function<T, V> valueExtractor) {
        return stream(entities)
                .collect(Collectors.toMap(keyExtractor, valueExtractor));
    }

    public static <T extends AbstractNamedEntity> Map<UUID, String> toIdNameEntityMap(Iterable<T> entities) {
        return stream(entities)
                .collect(Collectors.toMap(AbstractNamedEntity::getId, AbstractNamedEntity::getName));
    }

    public static <S, T> Map<S, List<T>> toEntityListMap(Iterable<T> entities,
                                                         Function<T, S> keyExtractor) {
        return stream(entities)
                .collect(Collectors.groupingBy(keyExtractor));
    }

    public static <T> List<T> toEntityList(Iterable<T> entities) {
        return stream(entities)
                .collect(Collectors.toList());
    }

    /**
     * Map entities from list to another type.
     *
     * @param entities entities list
     * @param clazz    convert class
     * @param <T>      entities type
     * @param <R>      convert type
     * @return result list
     */
    public static <T, R> List<R> mapToClazz(Iterable<T> entities, Class<R> clazz) {
        return stream(entities)
                .map(entity -> modelMapper.map(entity, clazz))
                .collect(Collectors.toList());
    }

    /**
     * Map entity to another type.
     *
     * @param entity entity
     * @param clazz  convert class
     * @param <T>    entities type
     * @param <R>    convert type
     * @return result list
     */
    public static <T, R> R mapToClazz(T entity, Class<R> clazz) {
        return modelMapper.map(entity, clazz);
    }

    /**
     * Extract specified field from entities list.
     *
     * @param entities  entities list
     * @param extractor extract function
     * @param <T>       entities type
     * @param <R>       convert type
     * @return result set
     */
    public static <T, R> Set<R> extractFields(Collection<T> entities, Function<T, R> extractor) {
        if (entities == null) {
            return Collections.emptySet();
        }

        return entities.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    /**
     * Map entities by provided map function.
     *
     * @param entities input entities
     * @param mapFunc  map function
     * @param <T>      input entities type
     * @param <R>      output entities type
     * @return list of mapped entities
     */
    public static <T, R> List<R> map(Collection<T> entities, Function<T, R> mapFunc) {
        return entities.stream()
                .map(mapFunc)
                .collect(Collectors.toList());
    }

    /**
     * Filter list with specified keys.
     *
     * @param entities    input entities list
     * @param containKeys filter entities keys
     * @param <T>         entity type
     * @return result list
     */
    public static <T extends AbstractNamedEntity> List<T> filterList(Collection<T> entities,
                                                                     Collection<UUID> containKeys) {
        return entities.stream()
                .filter(entity -> containKeys.contains(entity.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Filter list by specified condition.
     *
     * @param entities  input entities list
     * @param predicate filter condition
     * @param <T>       entity type
     * @return result list
     */
    public static <T extends AbstractNamedEntity> List<T> filterList(Collection<T> entities,
                                                                     Predicate<T> predicate) {
        return entities.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * Filter list with specified keys.
     *
     * @param entities    input entities list
     * @param containKeys filter entities keys
     * @param <T>         entity type
     * @return result list
     */
    public static <T> List<T> filterList(Collection<T> entities,
                                         Function<T, UUID> entityKeyExtractFunc,
                                         Collection<UUID> containKeys) {
        return entities.stream()
                .filter(entity -> containKeys.contains(entityKeyExtractFunc.apply(entity)))
                .collect(Collectors.toList());
    }

    /**
     * Filter list by specified condition.
     *
     * @param entities  input entities list
     * @param predicate filter condition
     * @param <T>       entity type
     * @return result list
     */
    public static <T extends AbstractNamedEntity> T find(Collection<T> entities, Predicate<T> predicate) {
        return entities.stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to find element in list"));
    }

    /**
     * Get list of entities from corresponding map.
     *
     * @param ids         entities identifiers
     * @param entitiesMap entities map
     * @return list of entities
     */
    public static <T> List<T> getEntitiesFromMap(Set<UUID> ids, Map<UUID, T> entitiesMap) {
        return ids.stream()
                .map(entitiesMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Find and return first element in the list.
     *
     * @param elements list elements
     * @param <T>      collection type
     * @return first element
     */
    public <T> T getFirstElem(List<T> elements) {
        if (!isEmpty(elements)) {
            return elements.get(0);
        }
        log.error("Failed to find first list element");
        throw new IllegalStateException("Failed to find first list element");
    }

    /**
     * Help method to distinct by key during filtering.
     * @param keyExtractor key extractor
     * @param <T> type
     * @return predicate
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
