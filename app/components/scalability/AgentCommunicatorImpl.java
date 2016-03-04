/*
 * Copyright (c) 2014-2015 University of Ulm
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package components.scalability;

import de.uniulm.omi.cloudiator.visor.client.ClientBuilder;
import de.uniulm.omi.cloudiator.visor.client.ClientController;
import de.uniulm.omi.cloudiator.visor.client.entities.*;
import models.MonitorInstance;
import models.RawMonitor;
import models.generic.ExternalReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Frank on 01.09.2015.
 */
public class AgentCommunicatorImpl implements AgentCommunicator {
    private final String protocol;
    private final String ip;
    private final int port;
    private final ClientController<Monitor> controller;

    public AgentCommunicatorImpl(String protocol, String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.protocol = protocol;

        //get the controller for the cloud entity
        this.controller =
            ClientBuilder.getNew()
                // the base url
                .url(protocol + "://" + ip + ":" + port)
                    // the entity to get the controller for.
                .build(Monitor.class);

    }

    /*
    TODO This probably won't work...
     */
    @Override public void removeSensorMonitor(String className, String metricName, long interval, TimeUnit unit){
        SensorMonitor monitor = (new SensorMonitorBuilder())
                .sensorClassName(className)
                .metricName(metricName)
                .interval(interval, unit).build();

        //remove a Monitor
        controller.delete(monitor);
    }

    @Override public void addSensorMonitor(String idMonitorInstance, String className, String metricName, long interval, TimeUnit unit){
        SensorMonitor monitor = (new SensorMonitorBuilder())
                .sensorClassName(className)
                .metricName(metricName)
                .interval(interval, unit)
                .addMonitorContext("monitorinstance", idMonitorInstance).build();

        //create a new Monitor
        monitor = (SensorMonitor) controller.create(monitor);
    }

    /*
    TODO This probably won't work...
     */
    @Override public void removeSensorMonitorForComponent(String className, String metricName, long interval, TimeUnit unit, String componentId){
        SensorMonitor monitor =
                (new SensorMonitorBuilder())
                        .sensorClassName(className)
                        .metricName(metricName)
                        .interval(interval, unit)
                        .addMonitorContext("component", componentId).build();

        //remove a Monitor
        controller.delete(monitor);
    }

    @Override public void removeSensorMonitor(SensorMonitor monitor) {
        //remove a Monitor
        controller.delete(monitor);
    }


    @Override public void addSensorMonitorForComponent(String idMonitorInstance, String className, String metricName, long interval, TimeUnit unit, String componentId){
        SensorMonitor monitor = (new SensorMonitorBuilder()).sensorClassName(className).metricName(
            metricName).interval(interval, unit).addMonitorContext("component", componentId).addMonitorContext("monitorinstance", idMonitorInstance).build();

        //create a new Monitor
        controller.create(monitor);
    }

    @Override public List<SensorMonitor> getSensorMonitorWithSameValues(String className, String metricName, String componentName) {
        List<Monitor> monitors = controller.getList();
        List<SensorMonitor> result = new ArrayList<SensorMonitor>();

        for(Monitor m : monitors){
            if(m instanceof SensorMonitor) {
                SensorMonitor sm = (SensorMonitor)m;
                if (sm.getSensorClassName().equals(className) &&
                        sm.getMetricName().equals(metricName) &&
                        checkForComponentContext(componentName, sm.getMonitorContext())) {
                    result.add(sm);
                }
            }
        }

        return result;
        //return new ArrayList<Monitor>();
    }

    @Override public void updateSensorMonitor(MonitorInstance mi) {
        List<Monitor> mons = controller.getList();

        for(Monitor m : mons){
            if(m instanceof SensorMonitor){
                SensorMonitor sm = (SensorMonitor)m;

                if (hasSameContext(sm, "monitorinstance", mi.getId().toString())){
                    controller.update(copyValueFromMonitorInstance(sm, mi));
                }
            }
        }
    }

    @Override public boolean hasSameContext(Monitor mon, String contextKey, String contextValue){
        for(Map.Entry entry : mon.getMonitorContext().entrySet()){
            if(entry.getKey().equals(contextKey) && entry.getValue().equals(contextValue)){
                return true;
            }
        }

        return false;
    }

    @Override public SensorMonitor copyValueFromMonitorInstance(SensorMonitor m, MonitorInstance mi){
        for(Map.Entry entry : m.getMonitorContext().entrySet()){
            if(entry.getKey().equals("monitorinstance")){
                entry.setValue(mi.getId().toString());
            }
        }

        for(ExternalReference er : mi.getExternalReferences()){
            m.getMonitorContext().put("er_" + er.getId(), er.getReference());
        }

        m.setSensorClassName(((RawMonitor)mi.getMonitor()).getSensorDescription().getClassName());
        m.setMetricName(((RawMonitor)mi.getMonitor()).getSensorDescription().getMetricName());
        m.setInterval(new Interval(((RawMonitor)mi.getMonitor()).getSchedule().getInterval(), ((RawMonitor)mi.getMonitor()).getSchedule().getTimeUnit().toString()));
        return m;
    }

    @Override public int getPort() {
        return port;
    }

    @Override public String getIp() {
        return ip;
    }

    @Override public String getProtocol() {
        return protocol;
    }

    private boolean checkForComponentContext(String componentName, Map<String, String> contexts) {
        boolean isComponent = false;

        if (componentName == null || componentName.equals("")){
            isComponent = true;
        } else {
            for(Map.Entry entry : contexts.entrySet()){
                if(entry.getKey().equals("component") && entry.getValue().equals(componentName)){
                    isComponent = true;
                }
            }
        }

        return isComponent;
    }
}
