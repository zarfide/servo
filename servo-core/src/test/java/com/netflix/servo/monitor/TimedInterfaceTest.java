/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.monitor;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class TimedInterfaceTest  {
    /**
     * Dummy interface to test our timer
     */
    private static interface IDummy {
        void method1();
        boolean method2(int n);
        Object method3(Object a, Object b);
    }

    private static class DummyImpl implements IDummy {
        private void sleep(int ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {
            }
        }

        @Override
        public void method1() {
            sleep(5);
        }

        @Override
        public boolean method2(int n) {
            sleep(15);
            return n > 0;
        }

        @Override
        public Object method3(Object a, Object b) {
            sleep(30);
            return a;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTimedInterface() {
        final IDummy dummy = TimedInterface.newProxy(IDummy.class, new DummyImpl(), "id");

        // you'd register the CompositeMonitor as:
        DefaultMonitorRegistry.getInstance().register((CompositeMonitor)dummy);

        for (int i = 0; i < 42; i++) {
            dummy.method1();
            if (i % 2 == 0) {
                dummy.method2(i);
            }
        }

        final CompositeMonitor<Long> compositeMonitor = (CompositeMonitor<Long>) dummy;
        final List<Monitor<?>> monitors = compositeMonitor.getMonitors();
        assertEquals(monitors.size(), 2);
        assertEquals(compositeMonitor.getValue().longValue(), 2L);

        final TagList tagList = BasicTagList.copyOf(
                new BasicTag(TimedInterface.CLASS_TAG, "DummyImpl"),
                new BasicTag(TimedInterface.INTERFACE_TAG, "IDummy"),
                new BasicTag(TimedInterface.ID_TAG, "id"));
        final MonitorConfig expectedConfig = MonitorConfig.builder(TimedInterface.TIMED_INTERFACE)
                .withTags(tagList).build();

        assertEquals(compositeMonitor.getConfig(), expectedConfig);

        for (Monitor<?> monitor: monitors) {
            final MonitorConfig config = monitor.getConfig();
            final String method = config.getName();
            final MonitorConfig expected = MonitorConfig.builder(method).withTags(tagList).build();
            assertEquals(config, expected);
            if (method.equals("method1")) {
                assertEquals(((Monitor<Long>)monitor).getValue().longValue(), 5);
            } else {
                assertEquals(method, "method2");
                assertEquals(((Monitor<Long>)monitor).getValue().longValue(), 15);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTimedInterfaceNoId() {
        final IDummy dummy = TimedInterface.newProxy(IDummy.class, new DummyImpl());

        // you'd register the CompositeMonitor as:
        DefaultMonitorRegistry.getInstance().register((CompositeMonitor)dummy);

        final CompositeMonitor<Long> compositeMonitor = (CompositeMonitor<Long>) dummy;
        final TagList tagList = BasicTagList.copyOf(
                new BasicTag(TimedInterface.CLASS_TAG, "DummyImpl"),
                new BasicTag(TimedInterface.INTERFACE_TAG, "IDummy"));
        final MonitorConfig expectedConfig = MonitorConfig.builder(TimedInterface.TIMED_INTERFACE)
                .withTags(tagList).build();
        assertEquals(compositeMonitor.getConfig(), expectedConfig);
    }
}
