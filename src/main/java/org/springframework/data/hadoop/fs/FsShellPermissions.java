/*
 * Copyright 2011-2012 the original author or authors.
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
package org.springframework.data.hadoop.fs;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsShell;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class for accessing Hadoop FsShellPermissions (which is not public) 
 * without having to duplicate its code. 
 * 
 * @author Costin Leau
 */
abstract class FsShellPermissions {

	enum Op {
		CHOWN("-chown"), CHMOD("-chmod"), CHGRP("-chgrp");

		private final String cmd;
		
		Op(String cmd){
			this.cmd = cmd;
		}

		public String getCmd() {
			return cmd;
		}
	}

	// TODO: move this into Spring Core (but add JDK 1.5 compatibility first)
	static <T> T[] concatAll(T[] first, T[]... rest) {
		// can add some sanity checks
		int totalLength = first.length;
		for (T[] array : rest) {
			totalLength += array.length;
		}
		T[] result = Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (T[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		return result;
	}

	static void changePermissions(FileSystem fs, Configuration config, Op op, boolean recursive, String group, String... uris) {
		String[] argvs = new String[0];

		if (recursive) {
			ObjectUtils.addObjectToArray(argvs, "-R");
		}

		argvs = concatAll(argvs, new String[] { group }, uris);

		Object[] args = new Object[] { fs, op.getCmd(), argvs, 0, new FsShell(config) };

		Class<?> cls = ClassUtils.resolveClassName("org.apache.hadoop.fs.FsShellPermissions", config.getClass().getClassLoader());
		Method m = ReflectionUtils.findMethod(cls, "changePermissions", FileSystem.class, String.class, String[].class, int.class, FsShell.class);
		ReflectionUtils.makeAccessible(m);
		ReflectionUtils.invokeMethod(m, null, args);
	}
}
