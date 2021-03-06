package org.rapidoid.cache;

/*
 * #%L
 * rapidoid-commons
 * %%
 * Copyright (C) 2014 - 2016 Nikolche Mihajlovski and contributors
 * %%
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
 * #L%
 */

import org.rapidoid.RapidoidThing;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.cache.impl.CacheDSL;
import org.rapidoid.cache.impl.ConcurrentCacheAtom;
import org.rapidoid.lambda.Mapper;

import java.util.concurrent.Callable;

@Authors("Nikolche Mihajlovski")
@Since("5.3.0")
public class Cache extends RapidoidThing {

	public static <K, V> CacheDSL<K, V> of(Mapper<K, V> of) {
		return new CacheDSL<K, V>().of(of);
	}

	public static <K, V> CacheDSL<K, V> of(Class<K> keyClass, Class<V> valueClass) {
		return new CacheDSL<>();
	}

	public static <T> CacheAtom<T> atom(Callable<T> loader, long ttlInMs) {
		return new ConcurrentCacheAtom<>(loader, ttlInMs);
	}

}
