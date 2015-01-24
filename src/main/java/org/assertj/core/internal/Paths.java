/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2012-2014 the original author or authors.
 */
package org.assertj.core.internal;

import org.assertj.core.api.AssertionInfo;
import org.assertj.core.util.PathsException;
import org.assertj.core.util.VisibleForTesting;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.isSymbolicLink;
import static java.nio.file.Files.notExists;
import static org.assertj.core.error.ShouldBeAbsolutePath.shouldBeAbsolutePath;
import static org.assertj.core.error.ShouldBeCanonicalPath.shouldBeCanonicalPath;
import static org.assertj.core.error.ShouldBeDirectory.shouldBeDirectory;
import static org.assertj.core.error.ShouldBeNormalized.shouldBeNormalized;
import static org.assertj.core.error.ShouldBeRegularFile.shouldBeRegularFile;
import static org.assertj.core.error.ShouldBeSymbolicLink.shouldBeSymbolicLink;
import static org.assertj.core.error.ShouldEndWithPath.shouldEndWith;
import static org.assertj.core.error.ShouldExist.shouldExist;
import static org.assertj.core.error.ShouldExistNoFollow.shouldExistNoFollow;
import static org.assertj.core.error.ShouldHaveNoParent.shouldHaveNoParent;
import static org.assertj.core.error.ShouldHaveParent.shouldHaveParent;
import static org.assertj.core.error.ShouldNotExist.shouldNotExist;
import static org.assertj.core.error.ShouldStartWithPath.shouldStartWith;

/**
 * Core assertion class for {@link Path} assertions
 */
public class Paths
{
  @VisibleForTesting
  public static final String IOERROR_FORMAT = "I/O error attempting to process assertion for path: <%s>";

  private static final Paths INSTANCE = new Paths();

  @VisibleForTesting
  Failures failures = Failures.instance();

  public static Paths instance() {
	return INSTANCE;
  }

  public void assertExists(final AssertionInfo info, final Path actual) {
	assertNotNull(info, actual);
	if (!exists(actual)) throw failures.failure(info, shouldExist(actual));
  }

  public void assertExistsNoFollowLinks(final AssertionInfo info, final Path actual) {
	assertNotNull(info, actual);
	if (!exists(actual, LinkOption.NOFOLLOW_LINKS)) throw failures.failure(info, shouldExistNoFollow(actual));
  }

  public void assertDoesNotExist(final AssertionInfo info, final Path actual)
  {
	assertNotNull(info, actual);
	if (!notExists(actual, LinkOption.NOFOLLOW_LINKS)) throw failures.failure(info, shouldNotExist(actual));
  }

  public void assertIsRegularFile(final AssertionInfo info, final Path actual) {
	assertExists(info, actual);
	if (!isRegularFile(actual)) throw failures.failure(info, shouldBeRegularFile(actual));
  }

  public void assertIsDirectory(final AssertionInfo info, final Path actual)
  {
	assertExists(info, actual);
	if (!isDirectory(actual)) throw failures.failure(info, shouldBeDirectory(actual));
  }

  public void assertIsSymbolicLink(final AssertionInfo info, final Path actual) {
	assertExistsNoFollowLinks(info, actual);
	if (!isSymbolicLink(actual)) throw failures.failure(info, shouldBeSymbolicLink(actual));
  }

  public void assertIsAbsolute(final AssertionInfo info, final Path actual) {
	assertNotNull(info, actual);
	if (!actual.isAbsolute()) throw failures.failure(info, shouldBeAbsolutePath(actual));
  }

  public void assertIsNormalized(final AssertionInfo info, final Path actual) {
	assertNotNull(info, actual);
	if (!actual.normalize().equals(actual)) throw failures.failure(info, shouldBeNormalized(actual));
  }

  public void assertIsCanonical(final AssertionInfo info, final Path actual) {
	assertNotNull(info, actual);
	try {
	  if (!actual.equals(actual.toRealPath())) throw failures.failure(info, shouldBeCanonicalPath(actual));
	} catch (IOException e) {
	  throw new PathsException(String.format(IOERROR_FORMAT, actual), e);
	}
  }

  public void assertHasParent(final AssertionInfo info, final Path actual, final Path expected) {
	assertNotNull(info, actual);
	checkExpectedParentPathIsNotNull(expected);

	final Path canonicalActual;
	try {
	  canonicalActual = actual.toRealPath();
	} catch (IOException e) {
	  throw new PathsException("failed to resolve actual", e);
	}

	final Path canonicalExpected;
	try {
	  canonicalExpected = expected.toRealPath();
	} catch (IOException e) {
	  throw new PathsException("failed to resolve path argument", e);
	}

	final Path actualParent = canonicalActual.getParent();
	if (actualParent == null) throw failures.failure(info, shouldHaveParent(actual, expected));
	if (!actualParent.equals(canonicalExpected))
	  throw failures.failure(info, shouldHaveParent(actual, actualParent, expected));
  }

  private static void checkExpectedParentPathIsNotNull(final Path expected) {
	if (expected == null) throw new NullPointerException("expected parent path should not be null");
  }

  public void assertHasParentRaw(final AssertionInfo info, final Path actual, final Path expected) {
	assertNotNull(info, actual);
	checkExpectedParentPathIsNotNull(expected);

	final Path actualParent = actual.getParent();
	if (actualParent == null) throw failures.failure(info, shouldHaveParent(actual, expected));
	if (!actualParent.equals(expected))
	  throw failures.failure(info, shouldHaveParent(actual, actualParent, expected));
  }

  public void assertHasNoParent(final AssertionInfo info, final Path actual) {
	assertNotNull(info, actual);
	try {
	  final Path canonicalActual = actual.toRealPath();
	  if (canonicalActual.getParent() != null) throw failures.failure(info, shouldHaveNoParent(actual));
	} catch (IOException e) {
	  throw new PathsException("cannot resolve actual path", e);
	}
  }

  public void assertHasNoParentRaw(final AssertionInfo info, final Path actual) {
	assertNotNull(info, actual);
	if (actual.getParent() != null) throw failures.failure(info, shouldHaveNoParent(actual));
  }

  public void assertStartsWith(final AssertionInfo info, final Path actual, final Path start) {
	assertNotNull(info, actual);
	assertExpectedStartPathIsNotNull(start);

	final Path canonicalActual;
	try {
	  canonicalActual = actual.toRealPath();
	} catch (IOException e) {
	  throw new PathsException("failed to resolve actual", e);
	}

	final Path canonicalOther;
	try {
	  canonicalOther = start.toRealPath();
	} catch (IOException e) {
	  throw new PathsException("failed to resolve path argument", e);
	}

	if (!canonicalActual.startsWith(canonicalOther)) throw failures.failure(info, shouldStartWith(actual, start));
  }

  private static void assertExpectedStartPathIsNotNull(final Path start) {
	if (start == null) throw new NullPointerException("the expected start path should not be null");
  }

  private static void assertExpectedEndPathIsNotNull(final Path end) {
	if (end == null) throw new NullPointerException("the expected end path should not be null");
  }

  public void assertStartsWithRaw(final AssertionInfo info, final Path actual, final Path other) {
	assertNotNull(info, actual);
	assertExpectedStartPathIsNotNull(other);
	if (!actual.startsWith(other)) throw failures.failure(info, shouldStartWith(actual, other));
  }

  public void assertEndsWith(final AssertionInfo info, final Path actual, final Path end) {
	assertNotNull(info, actual);
	assertExpectedEndPathIsNotNull(end);
	try {
	  final Path canonicalActual = actual.toRealPath();
	  if (!canonicalActual.endsWith(end.normalize())) throw failures.failure(info, shouldEndWith(actual, end));
	} catch (IOException e) {
	  throw new PathsException("cannot resolve actual path", e);
	}
  }

  public void assertEndsWithRaw(final AssertionInfo info, final Path actual, final Path end) {
	assertNotNull(info, actual);
	assertExpectedEndPathIsNotNull(end);
	if (!actual.endsWith(end)) throw failures.failure(info, shouldEndWith(actual, end));
  }

  private static void assertNotNull(final AssertionInfo info, final Path actual) {
	Objects.instance().assertNotNull(info, actual);
  }
}