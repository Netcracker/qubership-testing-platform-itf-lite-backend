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

package org.qubership.atp.itf.lite.backend.service;

import static org.qubership.atp.auth.springbootstarter.utils.ReflectionUtils.getGenericClassSimpleName;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CrudService<T extends AbstractEntity> implements IdentifiedService {

    protected abstract JpaRepository<T, UUID> repository();

    @Override
    public <E> boolean isEntityExists(E entity) {
        return repository().existsById((UUID) entity);
    }

    /**
     * Get entity by specified identifier.
     *
     * @param id entity identifier
     * @return entity
     */
    public T get(UUID id) {
        return repository().findById(id).orElseThrow(() -> {
            String entityName = getGenericClassSimpleName(this);
            log.error("Failed to found {} entity with id: {}", entityName, id);
            return new AtpEntityNotFoundException(entityName, id);
        });
    }

    public T save(T entity) {
        return repository().save(entity);
    }

    public List<T> getAll() {
        return repository().findAll();
    }

    public List<T> saveAll(List<T> entities) {
        return repository().saveAll(entities);
    }

    public void delete(T entity) {
        repository().delete(entity);
    }

    public void deleteByEntities(Iterable<T> entities) {
        repository().deleteAll(entities);
    }

}
