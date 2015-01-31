package br.com.caelum.vraptor.mvc;

import br.com.caelum.vraptor.controller.HttpMethod;
import br.com.caelum.vraptor.http.route.*;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import javax.enterprise.inject.Specializes;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static com.google.common.collect.Collections2.filter;
import static java.util.Arrays.asList;

@Specializes
public class MvcRoutesParser extends PathAnnotationRoutesParser {


	private Map<Class<? extends Annotation>, HttpMethod> methodsMap;
	private Router router;

	/**
	 * @deprecated cdi only
	 */
	protected MvcRoutesParser() {
	}

	@Inject
	public MvcRoutesParser(Router router) {
		super(router);
		this.router = router;
		this.methodsMap = new HashMap<>();
		methodsMap.put(GET.class, HttpMethod.GET);
		methodsMap.put(POST.class, HttpMethod.POST);
		methodsMap.put(PUT.class, HttpMethod.PUT);
		methodsMap.put(DELETE.class, HttpMethod.DELETE);
	}

	@Override
	protected List<Route> registerRulesFor(Class<?> baseType) {
		List<Route> routes = new ArrayList<>();
		List<Method> methods = asList(baseType.getMethods());
		Collection<Method> annotatedMethods = filter(methods, annotatedWithPath());
		for (Method annotatedMethod : annotatedMethods) {
			Path annotation = annotatedMethod.getAnnotation(Path.class);
			String uri = annotation.value();
			RouteBuilder builder = router.builderFor(uri);

			Set<HttpMethod> httpMethods = httpMethodsFor(annotatedMethod);

			builder.with(httpMethods);

			builder.is(baseType, annotatedMethod);
			Route route = builder.build();
			routes.add(route);
		}

		return routes;
	}

	private Set<HttpMethod> httpMethodsFor(Method annotatedMethod) {
		Annotation[] annotations = annotatedMethod.getAnnotations();
		return vraptorHttpMethodsFor(asList(annotations));
	}

	private Set<HttpMethod> vraptorHttpMethodsFor(List<Annotation> annotations) {
		Set<Class<? extends Annotation>> allJaxRsMethods = methodsMap.keySet();
		Collection<Annotation> jaxRsAnnotationsPresent = filter(annotations, new Predicate<Annotation>() {
			@Override
			public boolean apply(Annotation input) {
				return allJaxRsMethods.contains(input.annotationType());
			}
		});

		Collection<HttpMethod> methods = Collections2.transform(jaxRsAnnotationsPresent, new Function<Annotation, HttpMethod>() {
			@Override
			public HttpMethod apply(Annotation input) {
				return methodsMap.get(input.annotationType());
			}
		});
		return new HashSet<>(methods);
	}

	private Predicate<Method> annotatedWithPath() {
		return new Predicate<Method>() {
			@Override
			public boolean apply(Method method) {
				return method.isAnnotationPresent(Path.class);
			}
		};
	}

}
