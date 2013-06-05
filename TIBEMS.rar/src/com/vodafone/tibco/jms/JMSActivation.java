/*
 * VODAFONE Global
 * 
 * Distribute under LGPL license.
 * See terms of license at gnu.org.
 * 
 * 
 */
package com.vodafone.tibco.jms;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.Context;
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;

import org.jboss.logging.Logger;
import org.jboss.resource.adapter.jms.JmsResourceAdapter;
import org.jboss.resource.adapter.jms.inflow.JmsActivation;
import org.jboss.resource.adapter.jms.inflow.JmsActivationSpec;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * A JMS Activation class.This overrides method to set up the EMS connection based on encrypted credentials.
 * 
 * @author cliff.dias@vodafone.com
 *
 */
public class JMSActivation extends org.jboss.resource.adapter.jms.inflow.JmsActivation {

	private static final Logger log = Logger.getLogger(JMSActivation.class);
	
	/** Whether delivery is active */
	protected SynchronizedBoolean deliveryActive;
	   
	
	public JMSActivation(JmsResourceAdapter arg0, MessageEndpointFactory arg1,
			JmsActivationSpec arg2) throws ResourceException {
		super(arg0, arg1, arg2);		
	}

	public boolean isTopic()
	{
		boolean isTopic = false;
		
		if(Topic.class.getName().equals(spec.getDestinationType()))
			isTopic = true;
			
		return isTopic;
	}



	/**
	 * Teardown the DLQ
	 */
	@Override
	protected void teardownDLQ()
	{
		log.debug("Removing DLQ " + this);
		try
		{
			if (dlqHandler != null)
				dlqHandler.teardown();
		}
		catch (Throwable t)
		{
			log.debug("Error tearing down the DLQ " + dlqHandler, t);
		}
		dlqHandler = null;
	}

	@Override
	protected void setupConnection(Context ctx) throws Exception
	{
		log.debug("setup connection " + this);

		String user = "";
		String pass = "";

		if((ctx.getEnvironment() != null) && (ctx.getEnvironment().get(Context.SECURITY_PRINCIPAL) != null))
		{
			user = (String)(ctx.getEnvironment().get(Context.SECURITY_PRINCIPAL));
		}

		if(ctx.getEnvironment() != null && ctx.getEnvironment().get(Context.SECURITY_CREDENTIALS) != null)
		{
			pass = (String)ctx.getEnvironment().get(Context.SECURITY_CREDENTIALS);
		}

		String clientID = spec.getClientId();
		if (isTopic())
			connection = setupTopicConnection(ctx, user, pass, clientID);
		else
			connection = setupQueueConnection(ctx, user, pass, clientID);

		log.debug("established connection " + this);
	}

	/**
	 * Setup the activation
	 * 
	 * @throws Exception for any error
	 */
	@Override
	protected void setup() throws Exception
	{
		log.debug("Setting up " + spec);

		String user = "";
		String pass = "";

		setupJMSProviderAdapter();
		Context ctx = adapter.getInitialContext();
		log.debug("Using context " + ctx.getEnvironment() + " for " + spec);

		if((ctx.getEnvironment() != null) && (ctx.getEnvironment().get(Context.SECURITY_PRINCIPAL) != null))
		{
			user = (String)(ctx.getEnvironment().get(Context.SECURITY_PRINCIPAL));
			spec.setDLQUser(user);
			
		}

		if(ctx.getEnvironment() != null && ctx.getEnvironment().get(Context.SECURITY_CREDENTIALS) != null)
		{
			pass = (String)ctx.getEnvironment().get(Context.SECURITY_CREDENTIALS);
			spec.setDLQPassword(pass);
		}

		try
		{
			setupDLQ(ctx);
			setupDestination(ctx);
			setupConnection(ctx);
		}
		finally
		{
			ctx.close();
		}
		setupSessionPool();

		log.debug("Setup complete " + this);
	}	   

	/**
	 * Start the activation
	 * 
	 * @throws ResourceException for any error
	 */
	@Override
	public void start() throws ResourceException
	{
		deliveryActive = new SynchronizedBoolean(true);
		ra.getWorkManager().scheduleWork(new SetupActivation());
	}

	// Private -------------------------------------------------------

	// Inner classes -------------------------------------------------

	/**
	 * Handles the setup
	 */
	private class SetupActivation implements Work
	{
		public void run()
		{
			try
			{
				setup();
			}
			catch (Throwable t)
			{
				handleFailure(t);
			}
		}

		public void release()
		{
		}
	}
	   
}
