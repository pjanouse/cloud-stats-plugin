/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Red Hat, Inc.
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

package org.jenkinsci.plugins.cloudstats.WidgetImpl

import hudson.model.Actionable
import hudson.slaves.Cloud
import org.jenkinsci.plugins.cloudstats.*;

def l = namespace(lib.LayoutTagLib)
def st = namespace("jelly:stapler")

assert my instanceof WidgetImpl

if (my.displayed) {
   CloudStatistics stats = CloudStatistics.get()
    def title = "<a href='${rootURL}/${stats.getUrlName()}'>Cloud Statistics</a>"
    l.pane(id: "cloud-stats", width: 2, title: title, style: "margin-bottom: 20px") {
        def index = stats.index
        index.healthByTemplate().each { String cloudName, Map templates ->
            tr {
                def score = index.cloudHealth(cloudName).getCurrent()
                td(colspan: 2) {
                    l.icon("class": "${score.weather.iconClassName} icon-sm", alt: score.weather.score)
                    st.nbsp()
                    text(cloudName)
                }
                td(score)
            }
            // If the only template is the fake one there is no reason to report it as it represents the cloud
            if (!(templates.size() == 1 && templates.get(null) != null)) {
                templates.each { String templateName, Health health ->
                    tr {
                        td()
                        td {
                            l.icon("class": "${health.current.weather.iconClassName} icon-sm", alt: health.current.weather.score)
                            st.nbsp()
                            text(templateName)
                        }
                        td(health.current)
                    }
                }
            }
        }
    }
}
