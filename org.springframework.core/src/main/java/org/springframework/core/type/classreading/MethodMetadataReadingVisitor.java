/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type.classreading;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodAdapter;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.asm.commons.EmptyVisitor;
import org.springframework.core.type.MethodMetadata;

public class MethodMetadataReadingVisitor extends MethodAdapter implements MethodMetadata {

	private final Map<String, Map<String, Object>> attributesMap = new LinkedHashMap<String, Map<String, Object>>();

	private final Map<String, Set<String>> metaAnnotationMap = new LinkedHashMap<String, Set<String>>();

	private ClassLoader classLoader;
	private String name;
	private int access;
	private boolean isStatic;

	public MethodMetadataReadingVisitor(ClassLoader classLoader, String name, int access) {
		super(new EmptyVisitor());
		this.classLoader = classLoader;
		this.name = name;
		this.access = access;
		this.isStatic = ((access & Opcodes.ACC_STATIC) != 0);
	}

	public Map<String, Object> getAnnotationAttributes(String annotationType) {
		return this.attributesMap.get(annotationType);
	}

	public Set<String> getAnnotationTypes() {
		return this.attributesMap.keySet();
	}

	public String getMethodName() {
		return name;
	}

	public int getModifiers() {
		return access;
	}

	public boolean hasAnnotation(String annotationType) {
		return this.attributesMap.containsKey(annotationType);
	}
	
	public Set<String> getMetaAnnotationTypes(String annotationType) {
		return this.metaAnnotationMap.get(annotationType);
	}

	public boolean hasMetaAnnotation(String metaAnnotationType) {
		Collection<Set<String>> allMetaTypes = this.metaAnnotationMap.values();
		for (Set<String> metaTypes : allMetaTypes) {
			if (metaTypes.contains(metaAnnotationType)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isStatic() {
		return isStatic;
	}

	
	public Set<String> getAnnotationTypesWithMetaAnnotation(String metaAnnotationType) {

		///metaAnnotationMap.put(className, metaAnnotationTypeNames);
		Set<String> annotationTypes = new LinkedHashSet<String>();
		Set< Map.Entry<String, Set<String>> > metaValues = metaAnnotationMap.entrySet();
		Iterator<Map.Entry<String, Set<String>> > metaIterator = metaValues.iterator();
		while (metaIterator.hasNext())
		{
			Map.Entry<String, Set<String>> entry = metaIterator.next();
			String attributeType = entry.getKey();
			Set<String> metaAttributes = entry.getValue();
			if (metaAttributes.contains(metaAnnotationType))
			{
				annotationTypes.add(attributeType);
			}
		}
		return annotationTypes;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
		final String className = Type.getType(desc).getClassName();
		final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
		return new EmptyVisitor() {
			@Override
			public void visit(String name, Object value) {
				// Explicitly defined annotation attribute value.
				attributes.put(name, value);
			}
			@Override
			public void visitEnd() {
				try {
					Class<?> annotationClass = classLoader.loadClass(className);
					// Check declared default values of attributes in the annotation type.
					Method[] annotationAttributes = annotationClass.getMethods();
					for (int i = 0; i < annotationAttributes.length; i++) {
						Method annotationAttribute = annotationAttributes[i];
						String attributeName = annotationAttribute.getName();
						Object defaultValue = annotationAttribute.getDefaultValue();
						if (defaultValue != null && !attributes.containsKey(attributeName)) {
							attributes.put(attributeName, defaultValue);
						}
					}
					// Register annotations that the annotation type is annotated with.
					Annotation[] metaAnnotations = annotationClass.getAnnotations();
					Set<String> metaAnnotationTypeNames = new HashSet<String>();
					for (Annotation metaAnnotation : metaAnnotations) {
						metaAnnotationTypeNames.add(metaAnnotation.annotationType().getName());
					}
					metaAnnotationMap.put(className, metaAnnotationTypeNames);
				}
				catch (ClassNotFoundException ex) {
					// Class not found 
				}
				attributesMap.put(className, attributes);
			}
		};
	}


}
