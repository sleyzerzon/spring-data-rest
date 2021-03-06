package org.springframework.data.rest.repository.jpa;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.Metamodel;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.RepositoryMetadata;
import org.springframework.data.rest.repository.RepositoryQueryMethod;
import org.springframework.data.rest.repository.annotation.RestResource;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class JpaRepositoryMetadata implements RepositoryMetadata<JpaEntityMetadata> {

  private final String name;
  private final Class<?> repoClass;
  private final CrudRepository<Object, Serializable> repository;
  private final EntityInformation entityInfo;
  private final Map<String, RepositoryQueryMethod> queryMethods = new HashMap<String, RepositoryQueryMethod>();

  private String rel;
  private JpaEntityMetadata entityMetadata;

  @SuppressWarnings({"unchecked"})
  public JpaRepositoryMetadata(String name,
                               Class<?> domainType,
                               final Class<?> repoClass,
                               Repositories repositories,
                               EntityManager entityManager) {
    this.name = name;
    this.repoClass = repoClass;
    this.repository = repositories.getRepositoryFor(domainType);
    this.entityInfo = repositories.getEntityInformationFor(domainType);

    RestResource resourceAnno = repoClass.getAnnotation(RestResource.class);
    if (null != resourceAnno && StringUtils.hasText(resourceAnno.rel())) {
      rel = resourceAnno.rel();
    } else {
      rel = name;
    }

    for (Method method : repositories.getRepositoryInformationFor(domainType).getQueryMethods()) {
      String pathSeg = method.getName();
      RestResource methodResourceAnno = method.getAnnotation(RestResource.class);
      boolean methodExported = true;
      if (null != methodResourceAnno) {
        if (StringUtils.hasText(methodResourceAnno.path())) {
          pathSeg = methodResourceAnno.path();
        }
        methodExported = methodResourceAnno.exported();
      }
      if (methodExported) {
        ReflectionUtils.makeAccessible(method);
        queryMethods.put(pathSeg, new RepositoryQueryMethod(method));
      }
    }

    Metamodel metamodel = entityManager.getMetamodel();
    entityMetadata = new JpaEntityMetadata(repositories, metamodel.entity(entityInfo.getJavaType()));
  }

  @Override public String name() {
    return name;
  }

  @Override public String rel() {
    return rel;
  }

  @Override public Class<?> domainType() {
    return entityMetadata.type();
  }

  @Override public Class<?> repositoryClass() {
    return repoClass;
  }

  @Override public CrudRepository<Object, Serializable> repository() {
    return repository;
  }

  @Override public JpaEntityMetadata entityMetadata() {
    return entityMetadata;
  }

  @Override public RepositoryQueryMethod queryMethod(String key) {
    return queryMethods.get(key);
  }

  @Override public Map<String, RepositoryQueryMethod> queryMethods() {
    return Collections.unmodifiableMap(queryMethods);
  }

  @Override public String toString() {
    return "JpaRepositoryMetadata{" +
        "name='" + name + '\'' +
        ", repoClass=" + repoClass +
        ", repository=" + repository +
        ", entityInfo=" + entityInfo +
        ", queryMethods=" + queryMethods +
        ", entityMetadata=" + entityMetadata +
        '}';
  }

}
