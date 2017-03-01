/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.cloudstats;

import javax.annotation.Nonnull;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Health metric for a series of provisioning attempts.
 *
 * The actual statistics approach is left to the implementation and can be changed freely to fit provisioning reporting
 * and decision making. Use cases:
 *
 * - Report that particular cloud/template has its success rate low so it might require attention. ({@link #getOverall()})
 * - Provide data so plugins can pick the more successful cloud/template to fulfil the request. ({@link #getCurrent()})
 *
 * @author ogondza.
 */
public final class Health {

    private final @Nonnull List<ProvisioningActivity> samples;

    public Health(@Nonnull Collection<ProvisioningActivity> samples) {
        this.samples = new ArrayList<>(samples);
        Collections.sort(this.samples);
    }

    /**
     * Get Overall success rate of the series.
     */
    public Report getOverall() {
        int all = samples.size();
        float success = 0;
        for (ProvisioningActivity sample : samples) {
            if (sample.getStatus() != ProvisioningActivity.Status.FAIL) {
                success++;
            }
        }

        return new Report((success * 100 / all));
    }

    /**
     * Get projected probability the next provisioning attempt will succeed.
     *
     * Caution: There is a black magic involved.
     *
     * This implementation computes exponential average adjusting exponent based on the age of the data. This is
     * to give more wight to data that ware observed recently compared to older ones.
     */
    public Report getCurrent() {
        if (samples.isEmpty()) return new Report(Float.NaN);

        // Base the relative wights on the newest sample. It is important for older samples not to outweight the recent
        // ones but there is no reason to report bad score just because we do not have recent data.
        double start = samples.iterator().next().getStartedTimestamp();

        System.out.println(samples);

        double max = samples.size();
        double success = 0;
        for (ProvisioningActivity sample : samples) {
            double age = (start - sample.getStartedTimestamp()) / 1000 / 60 / 60;
            age += 1; // Avoid division by 0 increasing the age linearly
            assert age > 0: "Illegal sample age " + age;

            double increment = 1F / age;
            //System.out.printf("age=%s; sample=%s; inc=%s%n", start - sample.getStartedTimestamp(), sample.getStartedTimestamp(), increment);
            assert increment >= 0 && increment <= 1: "Illegal sample increment " + increment;
            if (sample.getStatus() != ProvisioningActivity.Status.FAIL) {
                success += increment;
            } else {
                success -= increment;
            }
        }

        // Map from range [-max;+max] to [0;+2max]
        success += max;
        max *=2;
        //System.out.printf("max=%s; start=%s; succ=%s; percentage=%s%n", max, start, success, success * 100 / max);
        return new Report((float) (success * 100 / max));
    }

    public static final class Report implements Comparable<Report> {

        private static final DecimalFormat FORMAT = new DecimalFormat("#.#'%'");

        private final float percent;

        public Report(@Nonnull float percent) {
            this.percent = percent;
        }

        public float getPercentage() {
            return percent;
        }

        @Override
        public int compareTo(@Nonnull Report o) {
            return Float.compare(this.percent, o.percent);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Report report = (Report) o;

            return Float.compare(report.percent, percent) == 0;
        }

        @Override
        public int hashCode() {
            return (percent != +0.0f ? Float.floatToIntBits(percent): 0);
        }

        @Override public String toString() {
            if (Float.isNaN(percent)) return "?";
            return FORMAT.format(percent);
        }
    }
}
