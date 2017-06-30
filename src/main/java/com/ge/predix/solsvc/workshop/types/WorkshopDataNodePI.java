/*
 * Copyright (c) 2014 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predix.solsvc.workshop.types;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

import org.iot.raspberry.grovepi.GroveAnalogIn;
import org.iot.raspberry.grovepi.GroveAnalogOut;
import org.iot.raspberry.grovepi.GroveDigitalIn;
import org.iot.raspberry.grovepi.GroveDigitalOut;
import org.iot.raspberry.grovepi.devices.*;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ge.dspmicro.machinegateway.types.PDataNode;
import com.ge.predix.solsvc.workshop.config.JsonDataNode;
import com.ge.predix.solsvc.workshop.config.TriggerNode;

/**
 * 
 * 
 * @author Predix Machine Sample
 */
public class WorkshopDataNodePI extends PDataNode
{
	
	private static final Logger               _logger             = LoggerFactory.getLogger(WorkshopDataNodePI.class);
	private String nodeType;

	private String nodePinType;
	
	private String nodePinDir;
	
	private String expression;
	
	private String nodeClass;
	
	private int nodePin;
	
	private String nodeName;
	
	private List<TriggerNode> triggerNodes;
	
	private GroveAnalogIn analogIn;
	
	private GroveAnalogOut analogOut;
	
	private GroveTemperatureAndHumiditySensor temperatureNode;
	
	private GroveLightSensor lightNode;
	
	private GroveRotarySensor rotaryNode;
	
	private GroveDigitalIn digitalIn;
	
	private GroveDigitalOut digitalOut;
	

		
    /**
	 * @param machineAdapterId -
	 * @param node -
	 */
	@SuppressWarnings("resource")
	public WorkshopDataNodePI(UUID machineAdapterId, JsonDataNode node, GrovePi4J pi) {
		super(machineAdapterId, node.getNodeName());
		this.nodeType = node.getNodeType();
		this.setTriggerNodes(node.getTriggerNodes());
		this.nodePinType = node.getNodePinType();
		this.nodePinDir = node.getNodePinDir();
		this.expression = node.getExpression();
		this.nodeClass = node.getNodeClass();
		this.nodePin = node.getNodePin();
		this.nodeName = node.getNodeName();
		int bufferSize = 1024;
		try {
			int nodePin = node.getNodePin();
			_logger.info("creating WorkshopDataNodePi: " + node.getNodeName() +"," +(this.nodePinType+":"+this.nodePinDir).toUpperCase());
			switch ((this.nodePinType+":"+this.nodePinDir).toUpperCase()) {
				case "ANALOG:IN" :
					switch ((this.nodeType.toUpperCase())){
						case "LIGHT":
							this.lightNode = new GroveLightSensor(pi, this.nodePin);
							break;
						case "ROTARYANGLE":
							this.rotaryNode = new GroveRotarySensor(pi, this.nodePin);
							break;
						default:
							break;
					}
					this.analogIn = pi.getAnalogIn(nodePin, bufferSize);
					break;
				case "ANALOG:OUT" :
					this.analogOut = pi.getAnalogOut(nodePin);
					break;
				case "DIGITAL:IN" :
					switch ((this.nodeType.toUpperCase())){
						case "TEMPERATURE":
							this.temperatureNode = new GroveTemperatureAndHumiditySensor(pi, this.nodePin, GroveTemperatureAndHumiditySensor.Type.DHT11);
							break;
						default:
							break;
					}
					this.digitalIn = pi.getDigitalIn(nodePin);
					break;
				case "DIGITAL:OUT" :
					this.digitalOut = pi.getDigitalOut(nodePin);
					break;
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Exception when building the nodes",e); //$NON-NLS-1$
		}
	}

	public double readValue() {
		double fvalue = 0.0f;
		try {
			switch ((this.nodePinType+":"+this.nodePinDir).toUpperCase()) {
			case "ANALOG:IN" :
				DecimalFormat df = new DecimalFormat("####.##"); //$NON-NLS-1$
				switch(this.getNodeType().toUpperCase()){
					case "LIGHT":
						fvalue = this.lightNode.get();
						break;
					case "ROTARYANGLE":
						GroveRotaryValue grv = this.rotaryNode.get();
						fvalue = grv.getSensorValue();
						break;
					default:
						fvalue = new Double(df.format(ByteBuffer.wrap(this.analogIn.get()).getFloat()));
						break;
				}
				break;
			case "DIGITAL:IN" :
				switch(this.getNodeType().toUpperCase()){
					case "TEMPERATURE":
						GroveTemperatureAndHumidityValue gtahv = this.temperatureNode.get();
						fvalue = gtahv.getTemperature();
						break;
					default:
						fvalue = this.digitalIn.get() ? 1.0 : 0.0;
						break;
				}
				break;
			}
			_logger.info("Node: " + this.nodeName + " value:" + String.valueOf(fvalue));
		}catch(Exception e){
			_logger.info("Exception in IO Read Value");
			
		}
		return fvalue;
	}
	
	public void writeValue(double value) {
		try {
			switch ((this.nodePinType+":"+this.nodePinDir).toUpperCase()) {
			case "ANALOG:OUT" :
				this.analogOut.set(value);
				break;
			case "DIGITAL:OUT" :
				this.digitalOut.set(value == 1.0);
				break;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
    /**
     * Node address to uniquely identify the node.
     */
    @Override
    public URI getAddress()
    {
        try
        {
            URI address = new URI("sample.subscription.adapter", null, "localhost", -1, "/" + getName(), null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return address;
        }
        catch (URISyntaxException e)
        {
        	_logger.error("Exception",e);
        	return null;
        }
    }

	/**
	 * @return -
	 */
	public String getNodeType() {
		return this.nodeType;
	}

	/**
	 * @param nodeType -
	 */
	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	

	/**
	 * @return -
	 */
	public GroveTemperatureAndHumiditySensor getTemperatureNode() {
		return this.temperatureNode;
	}

	/**
	 * @param tempNode -
	 */
	public void setTemperatureNode(GroveTemperatureAndHumiditySensor tempNode) {
		this.temperatureNode = tempNode;
	}

	public String getNodePinType() {
		return nodePinType;
	}

	public void setNodePinType(String nodePinType) {
		this.nodePinType = nodePinType;
	}

	public String getNodePinDir() {
		return nodePinDir;
	}

	public void setNodePinDir(String nodePinDir) {
		this.nodePinDir = nodePinDir;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getNodeClass() {
		return nodeClass;
	}

	public void setNodeClass(String nodeClass) {
		this.nodeClass = nodeClass;
	}

	public List<TriggerNode> getTriggerNodes() {
		return triggerNodes;
	}

	public void setTriggerNodes(List<TriggerNode> triggerNodes) {
		this.triggerNodes = triggerNodes;
	}

	public int getNodePin(){
		return this.nodePin;
	}
	
	public String getNodeName(){
		return this.nodeName;
	}
}
