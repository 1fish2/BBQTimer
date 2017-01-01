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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class PluralizationTest {
    @Parameters(name = "pluralization quantity for {0} -> {1}") // {index}
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { // {seconds, expectedQuantity} pairs
                {0, 0}, {60, 1}, {120, 2}, {6000, 100}, // whole minutes
                {30, 5}, {90, 5}, {150, 5}, // 0.5, 1.5, 2.5 minutes
        });
    }

    @Parameter
    public int seconds;

    @Parameter(value = 1)
    public int expectedQuantity;

    @Test
    public void enUsPlurals() {
        assertThat("pluralization quantity",
                MinutesChoices.secondsToMinutesPluralizationQuantity(seconds),
                is(expectedQuantity));
    }
}
