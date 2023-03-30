/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet.client;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.servlet.Filter;

import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.test.web.servlet.DispatcherServletCustomizer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient.Builder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * The main class for testing Spring MVC applications via {@link WebTestClient}
 * with {@link MockMvc} for server request handling.
 *
 * <p>Provides static factory methods and specs to initialize {@code MockMvc}
 * to which the {@code WebTestClient} connects to. For example:
 * <pre class="code">
 * MockMvcWebTestClient client = MockMvcWebTestClient.bindToController(myController)
 *         .controllerAdvice(myControllerAdvice)
 *         .validator(myValidator)
 *         .build()
 * </pre>
 *
 * <p>The client itself can also be configured. For example:
 * <pre class="code">
 * MockMvcWebTestClient client = MockMvcWebTestClient.bindToController(myController)
 *         .validator(myValidator)
 *         .configureClient()
 *         .baseUrl("/path")
 *         .build();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public interface MockMvcWebTestClient extends WebTestClient {
	
	/**
	 * Return a builder to mutate properties of this web test client.
	 */
	Builder mutate();
	
	/**
	 * Mutate the {@link MockMvcWebTestClient}, apply the given requestPostProcessor, and build
	 * a new instance. Essentially a shortcut for:
	 * <pre>
	 * mutate().requestPostProcessor(requestPostProcessor).build();
	 * </pre>
	 * @param requestPostProcessor the requestPostProcessor to apply
	 * @return the mutated test client
	 */
	MockMvcWebTestClient mutateWith(RequestPostProcessor requestPostProcessor);
	
	/**
	 * Mutate the {@link MockMvcWebTestClient}, apply the given configurer, and build
	 * a new instance. Essentially a shortcut for:
	 * <pre>
	 * mutate().apply(configurer).build();
	 * </pre>
	 * @param configurer the configurer to apply
	 * @return the mutated test client
	 */
	MockMvcWebTestClient mutateWith(MockMvcWebTestClientConfigurer configurer);

	
	/**
	 * Steps for customizing the {@link WebClient} used to test with,
	 * internally delegating to a
	 * {@link org.springframework.web.reactive.function.client.WebClient.Builder
	 * WebClient.Builder}.
	 */
	interface Builder extends WebTestClient.Builder {
		Builder mockMvc(MockMvc mockMvc);
		
		Builder requestPostProcessor(RequestPostProcessor requestPostProcessor);
		
		Builder requestPostProcessors(Consumer<List<RequestPostProcessor>> requestPostProcessorsConsumer);
		
		Builder requestBuilderCustomizer(Consumer<MockHttpServletRequestBuilder> requestBuilderCustomizer);
		
		Builder requestBuilderCustomizers(Consumer<List<Consumer<MockHttpServletRequestBuilder>>> requestBuilderCustomizersConsumer);
		
		Builder apply(MockMvcWebTestClientConfigurer configurer);
		
		/* Override methods to return covariant return type */
		@Override
		Builder baseUrl(String baseUrl);

		@Override
		Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory);

		@Override
		Builder defaultHeader(String headerName, String... headerValues);
		
		@Override
		Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer);

		@Override
		Builder defaultCookie(String cookieName, String... cookieValues);
		
		@Override
		Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);
		
		@Override
		Builder filter(ExchangeFilterFunction filter);
		
		@Override
		Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer);
		
		@Override
		Builder entityExchangeResultConsumer(Consumer<EntityExchangeResult<?>> consumer);
		
		@Override
		Builder codecs(Consumer<ClientCodecConfigurer> configurer);
		
		@Override
		Builder exchangeStrategies(ExchangeStrategies strategies);
		
		@Override
		@SuppressWarnings("deprecation")
		Builder exchangeStrategies(
				Consumer<ExchangeStrategies.Builder> configurer);
		
		@Override
		Builder responseTimeout(Duration timeout);
		
		@Override
		Builder apply(WebTestClientConfigurer configurer);
		
		/**
		 * Build the {@link WebTestClient} instance.
		 */
		@Override
		MockMvcWebTestClient build();
		
	}
	

	/**
	 * Begin creating a {@link WebTestClient} by providing the {@code @Controller}
	 * instance(s) to handle requests with.
	 * <p>Internally this is delegated to and equivalent to using
	 * {@link org.springframework.test.web.servlet.setup.MockMvcBuilders#standaloneSetup(Object...)}.
	 * to initialize {@link MockMvc}.
	 */
	static ControllerSpec bindToController(Object... controllers) {
		return new StandaloneMockMvcSpec(controllers);
	}

	/**
	 * Begin creating a {@link WebTestClient} by providing a
	 * {@link WebApplicationContext} with Spring MVC infrastructure and
	 * controllers.
	 * <p>Internally this is delegated to and equivalent to using
	 * {@link org.springframework.test.web.servlet.setup.MockMvcBuilders#webAppContextSetup(WebApplicationContext)}
	 * to initialize {@code MockMvc}.
	 */
	static MockMvcServerSpec<?> bindToApplicationContext(WebApplicationContext context) {
		return new ApplicationContextMockMvcSpec(context);
	}
	
	/**
	 * Begin creating a {@link WebTestClient} by providing an already
	 * initialized {@link MockMvc} instance to use as the server.
	 */
	@Deprecated // Changing the return type causes Spring Boot to throw in MockMvcAutoConfiguration
	static WebTestClient.Builder bindTo(MockMvc mockMvc) {
		return bindToMockMvc(mockMvc);
	}

	/**
	 * Begin creating a {@link WebTestClient} by providing an already
	 * initialized {@link MockMvc} instance to use as the server.
	 */
	static MockMvcWebTestClient.Builder bindToMockMvc(MockMvc mockMvc) {
		return new DefaultMockMvcWebTestClientBuilder(mockMvc);
	}

	/**
	 * This method can be used to apply further assertions on a given
	 * {@link ExchangeResult} based the state of the server response.
	 * <p>Normally {@link WebTestClient} is used to assert the client response
	 * including HTTP status, headers, and body. That is all that is available
	 * when making a live request over HTTP. However when the server is
	 * {@link MockMvc}, many more assertions are possible against the server
	 * response, e.g. model attributes, flash attributes, etc.
	 *
	 * <p>Example:
	 * <pre class="code">
	 * EntityExchangeResult&lt;Void&gt; result =
	 * 		webTestClient.post().uri("/people/123")
	 * 				.exchange()
	 * 				.expectStatus().isFound()
	 * 				.expectHeader().location("/persons/Joe")
	 * 				.expectBody().isEmpty();
	 *
	 * MockMvcWebTestClient.resultActionsFor(result)
	 * 		.andExpect(model().size(1))
	 * 		.andExpect(model().attributeExists("name"))
	 * 		.andExpect(flash().attributeCount(1))
	 * 		.andExpect(flash().attribute("message", "success!"));
	 * </pre>
	 * <p>Note: this method works only if the {@link WebTestClient} used to
	 * perform the request was initialized through one of bind method in this
	 * class, and therefore requests are handled by {@link MockMvc}.
	 */
	static ResultActions resultActionsFor(ExchangeResult exchangeResult) {
		Object serverResult = exchangeResult.getMockServerResult();
		if (!(serverResult instanceof MvcResult mvcResult)) {
			throw new IllegalArgumentException(
					"Result from mock server exchange must be an instance of MvcResult instead of " +
							(serverResult != null ? serverResult.getClass().getName() : "null"));
		}
		return new ResultActions() {
			@Override
			public ResultActions andExpect(ResultMatcher matcher) throws Exception {
				matcher.match(mvcResult);
				return this;
			}
			@Override
			public ResultActions andDo(ResultHandler handler) throws Exception {
				handler.handle(mvcResult);
				return this;
			}
			@Override
			public MvcResult andReturn() {
				return mvcResult;
			}
		};
	}


	/**
	 * Base specification for configuring {@link MockMvc}, and a simple facade
	 * around {@link ConfigurableMockMvcBuilder}.
	 *
	 * @param <B> a self reference to the builder type
	 */
	interface MockMvcServerSpec<B extends MockMvcServerSpec<B>> {

		/**
		 * Add a global filter.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#addFilters(Filter...)}.
		 */
		<T extends B> T filters(Filter... filters);

		/**
		 * Add a filter for specific URL patterns.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#addFilter(Filter, String...)}.
		 */
		<T extends B> T filter(Filter filter, String... urlPatterns);

		/**
		 * Define default request properties that should be merged into all
		 * performed requests such that input from the client request override
		 * the default properties defined here.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#defaultRequest(RequestBuilder)}.
		 */
		<T extends B> T defaultRequest(RequestBuilder requestBuilder);

		/**
		 * Define a global expectation that should <em>always</em> be applied to
		 * every response.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#alwaysExpect(ResultMatcher)}.
		 */
		<T extends B> T alwaysExpect(ResultMatcher resultMatcher);

		/**
		 * Whether to handle HTTP OPTIONS requests.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#dispatchOptions(boolean)}.
		 */
		<T extends B> T dispatchOptions(boolean dispatchOptions);

		/**
		 * Allow customization of {@code DispatcherServlet}.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#addDispatcherServletCustomizer(DispatcherServletCustomizer)}.
		 */
		<T extends B> T dispatcherServletCustomizer(DispatcherServletCustomizer customizer);

		/**
		 * Add a {@code MockMvcConfigurer} that automates MockMvc setup.
		 * <p>This is delegated to
		 * {@link ConfigurableMockMvcBuilder#apply(MockMvcConfigurer)}.
		 */
		<T extends B> T apply(MockMvcConfigurer configurer);

		/**
		 * Proceed to configure and build the test client.
		 */
		Builder configureClient();

		/**
		 * Shortcut to build the test client.
		 */
		MockMvcWebTestClient build();
	}


	/**
	 * Specification for configuring {@link MockMvc} to test one or more
	 * controllers directly, and a simple facade around
	 * {@link StandaloneMockMvcBuilder}.
	 */
	interface ControllerSpec extends MockMvcServerSpec<ControllerSpec> {

		/**
		 * Register {@link org.springframework.web.bind.annotation.ControllerAdvice}
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setControllerAdvice(Object...)}.
		 */
		ControllerSpec controllerAdvice(Object... controllerAdvice);

		/**
		 * Set the message converters to use.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setMessageConverters(HttpMessageConverter[])}.
		 */
		ControllerSpec messageConverters(HttpMessageConverter<?>... messageConverters);

		/**
		 * Provide a custom {@link Validator}.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setValidator(Validator)}.
		 */
		ControllerSpec validator(Validator validator);

		/**
		 * Provide a conversion service.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setConversionService(FormattingConversionService)}.
		 */
		ControllerSpec conversionService(FormattingConversionService conversionService);

		/**
		 * Add global interceptors.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#addInterceptors(HandlerInterceptor...)}.
		 */
		ControllerSpec interceptors(HandlerInterceptor... interceptors);

		/**
		 * Add interceptors for specific patterns.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#addMappedInterceptors(String[], HandlerInterceptor...)}.
		 */
		ControllerSpec mappedInterceptors(
				@Nullable String[] pathPatterns, HandlerInterceptor... interceptors);

		/**
		 * Set a ContentNegotiationManager.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setContentNegotiationManager(ContentNegotiationManager)}.
		 */
		ControllerSpec contentNegotiationManager(ContentNegotiationManager manager);

		/**
		 * Specify the timeout value for async execution.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setAsyncRequestTimeout(long)}.
		 */
		ControllerSpec asyncRequestTimeout(long timeout);

		/**
		 * Provide custom argument resolvers.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setCustomArgumentResolvers(HandlerMethodArgumentResolver...)}.
		 */
		ControllerSpec customArgumentResolvers(HandlerMethodArgumentResolver... argumentResolvers);

		/**
		 * Provide custom return value handlers.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setCustomReturnValueHandlers(HandlerMethodReturnValueHandler...)}.
		 */
		ControllerSpec customReturnValueHandlers(HandlerMethodReturnValueHandler... handlers);

		/**
		 * Set the HandlerExceptionResolver types to use.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setHandlerExceptionResolvers(HandlerExceptionResolver...)}.
		 */
		ControllerSpec handlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers);

		/**
		 * Set up view resolution.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setViewResolvers(ViewResolver...)}.
		 */
		ControllerSpec viewResolvers(ViewResolver... resolvers);

		/**
		 * Set up a single {@link ViewResolver} with a fixed view.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setSingleView(View)}.
		 */
		ControllerSpec singleView(View view);

		/**
		 * Provide the LocaleResolver to use.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setLocaleResolver(LocaleResolver)}.
		 */
		ControllerSpec localeResolver(LocaleResolver localeResolver);

		/**
		 * Provide a custom FlashMapManager.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setFlashMapManager(FlashMapManager)}.
		 */
		ControllerSpec flashMapManager(FlashMapManager flashMapManager);

		/**
		 * Enable URL path matching with parsed
		 * {@link org.springframework.web.util.pattern.PathPattern PathPatterns}.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setPatternParser(PathPatternParser)}.
		 */
		ControllerSpec patternParser(PathPatternParser parser);

		/**
		 * Whether to match trailing slashes.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setUseTrailingSlashPatternMatch(boolean)}.
		 * @deprecated as of 6.0, see
		 * {@link PathPatternParser#setMatchOptionalTrailingSeparator(boolean)}
		 */
		@Deprecated(since = "6.0")
		ControllerSpec useTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch);

		/**
		 * Configure placeholder values to use.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#addPlaceholderValue(String, String)}.
		 */
		ControllerSpec placeholderValue(String name, String value);

		/**
		 * Configure factory for a custom {@link RequestMappingHandlerMapping}.
		 * <p>This is delegated to
		 * {@link StandaloneMockMvcBuilder#setCustomHandlerMapping(Supplier)}.
		 */
		ControllerSpec customHandlerMapping(Supplier<RequestMappingHandlerMapping> factory);
	}

}
