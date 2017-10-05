/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jerry Morrison
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.onefishtwo.bbqtimer;

import android.support.annotation.NonNull;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;

/** Matcher to test that a List starts with the expected prefix. */
public class ListPrefixMatcher<E> extends FeatureMatcher<List<E>, List<E>> {
    final List<E> expectedPrefix;

    @NonNull
    public static <T> Matcher<List<T>> listPrefixMatcher(List<T> expectedPrefix) {
        return new ListPrefixMatcher<>(expectedPrefix);
    }

    @NonNull
    @SafeVarargs
    public static <T> Matcher<List<T>> listPrefixMatcher(T... expectedPrefix) {
        return new ListPrefixMatcher<>(Arrays.asList(expectedPrefix));
    }

    public ListPrefixMatcher(List<E> expectedPrefix) {
        super(is(expectedPrefix), "prefix ", "prefix");
        this.expectedPrefix = expectedPrefix;
    }

    @NonNull
    @Override
    protected List<E> featureValueOf(@NonNull List<E> actual) {
        return actual.subList(0, Math.min(expectedPrefix.size(), actual.size()));
    }
}
