/*
 * VODAFONE Global
 * 
 * Distribute under LGPL license.
 * See terms of license at gnu.org.
 * 
 * 
 */
package com.vodafone.tibco.jms;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import org.jboss.logging.Logger;
import org.jboss.resource.adapter.jms.JmsResourceAdapter;
import org.jboss.resource.adapter.jms.inflow.JmsActivationSpec;

/**
 * A resource adapter to allow Message Driven beans to connect to an EMS server
 * 
 * @author cliff.dias@vodafone.com
 *
 */
public class TibEMSResourceAdapter extends JmsResourceAdapter {

	/** The logger */
	private static final Logger log = Logger.getLogger(TibEMSResourceAdapter.class);

	private ConcurrentHashMap<ActivationSpec, JMSActivation> activations = new ConcurrentHashMap<ActivationSpec, JMSActivation>();

	@Override
	public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException
	{
		JMSActivation activation = new JMSActivation(this, endpointFactory, (JmsActivationSpec) spec);
		activations.put(spec, activation);
		activation.start();
	}
	
	@Override
	public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec)
	{

		JMSActivation activation = (JMSActivation) activations.remove(spec);
		if (activation != null)
			activation.stop();
	}

	@Override
	public void stop()
	{
		for (Iterator i = activations.entrySet().iterator(); i.hasNext();)
		{
			Map.Entry entry = (Map.Entry) i.next();
			try
			{
				JMSActivation activation = (JMSActivation) entry.getValue();
				activation.stop();
			}
			catch (Exception ignored)
			{
				log.debug("Ignored", ignored);
			}
			i.remove();
		}
	}	   
}
