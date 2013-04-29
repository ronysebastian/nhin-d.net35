package org.nhindirect.policy.tools.policyvalidate;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.nhindirect.policy.PolicyFilterFactory;
import org.nhindirect.policy.PolicyLexicon;
import org.nhindirect.policy.PolicyExpression;
import org.nhindirect.policy.PolicyFilter;
import org.nhindirect.policy.PolicyLexiconParser;
import org.nhindirect.policy.PolicyLexiconParserFactory;
import org.nhindirect.policy.PolicyRequiredException;
import org.nhindirect.policy.impl.machine.StackMachineCompiler;

///CLOVER:OFF
public class ValidatePanel extends JPanel 
{
	static final long serialVersionUID = -1058079566562354445L;
	
	protected FileField policyFileField;
	protected FileField certFileField;
	protected JTextArea reportText;
	
	protected JButton cmdValidate;
	
	public ValidatePanel()
	{
		super();
		
		initUI();
		
		addActions();
	}
	
	protected void initUI()
	{
		setLayout(new BorderLayout());
		setBorder(new CompoundBorder( 
                new SoftBevelBorder(BevelBorder.LOWERED), new EmptyBorder(5,5,5,5)) ); 
		
		// File Load Fields
		policyFileField = new FileField("Policy Definition File:", "");
		certFileField = new FileField("Certificate File: ", "");
		
		
		final JPanel filePanel = new JPanel(new GridLayout(1, 2));
		filePanel.add(policyFileField);
		filePanel.add(certFileField);
		
		this.add(filePanel, BorderLayout.NORTH);
		
		// Report panel
		final JLabel reportHeaderLabel = new JLabel("Validation Report");
		reportText = new JTextArea();
		reportText.setLineWrap(true);
		reportText.setWrapStyleWord(true);
		reportText.setEditable(false);
		
		
		JScrollPane scrollPane = new JScrollPane(reportText); 
		scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		final JPanel reportPanel = new JPanel(new BorderLayout());
		reportPanel.add(reportHeaderLabel, BorderLayout.NORTH);
		reportPanel.add(scrollPane, BorderLayout.CENTER);
		
		this.add(reportPanel, BorderLayout.CENTER);
		
		// Button Panel
		cmdValidate = new JButton("Validate");
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(cmdValidate);
		this.add(buttonPanel, BorderLayout.SOUTH);
		
	}
	
	private void addActions()
	{
		cmdValidate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				validateCert();
			}
		});
	}
	
	private void validateCert()
	{
		reportText.setText("");
		
		final File certFile = certFileField.getFile();
		final File policyFile = policyFileField.getFile();
		
		if (!certFile.exists())
		{
			JOptionPane.showMessageDialog(this,"Certificate file does not exist or cannot be found.", 
		 		    "Invalid Cert File", JOptionPane.ERROR_MESSAGE );
			return;
		}
		
		if (!policyFile.exists())
		{
			JOptionPane.showMessageDialog(this,"Policy file does not exist or cannot be found.", 
		 		    "Invalid Policy File", JOptionPane.ERROR_MESSAGE );
			
			return;
		}
		
		// load the certificate
		X509Certificate cert = null;
		InputStream policyInput = null;
		try
		{
			cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(FileUtils.openInputStream(certFile));    
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this,"Could not load certificate from file: " + e.getMessage(), 
		 		    "Invalid Cert File", JOptionPane.ERROR_MESSAGE );
			
			return;
		}
		
		try
		{
			// load the policy as an input stream
			policyInput = FileUtils.openInputStream(policyFile);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this,"Could not load policy from file: " + e.getMessage(), 
		 		    "Invalid Policy File", JOptionPane.ERROR_MESSAGE );
			
			return;
		}
		
		final DateFormat dateFormat = new SimpleDateFormat("EEE, MMM d yyyy HH:mm:ss", Locale.getDefault());
		final StringBuilder reportTextBuilder = new StringBuilder("Validation run at " + dateFormat.format(Calendar.getInstance(Locale.getDefault()).getTime())
				+ "\r\n\r\n");
		
		try
		{
			final PolicyLexiconParser parser = PolicyLexiconParserFactory.getInstance(PolicyLexicon.XML);
			final PolicyExpression policyExpression = parser.parse(policyInput);
		
			
			final org.nhindirect.policy.Compiler compiler = new StackMachineCompiler();
			compiler.setReportModeEnabled(true);
			final PolicyFilter filter = PolicyFilterFactory.getInstance(compiler);
			
			if (filter.isCompliant(cert, policyExpression) && compiler.getCompilationReport().isEmpty())			
				reportTextBuilder.append("Certificate is compliant with the provided policy.");
			else
			{
				reportTextBuilder.append("Certificate is NOT compliant with the provided policy.\r\n\r\n");
				
				final Collection<String> report = compiler.getCompilationReport();
				if (!report.isEmpty())
				{
					for (String reportEntry : report)
						reportTextBuilder.append(reportEntry + "\r\n");
				}
			}
		}
		catch (PolicyRequiredException e)
		{
			reportTextBuilder.append("Validation Successful\r\nCertificate is missing a required field\r\n\t" + e.getMessage());
		}
		catch (Exception e)
		{
			final ByteArrayOutputStream str = new ByteArrayOutputStream();
			final PrintStream printStr = new PrintStream(str);
			
			e.printStackTrace();
			
			e.printStackTrace(printStr);
			
			final String stackTrace = new String(str.toByteArray());
			
			reportTextBuilder.append("Validation Failed\r\nError compiling or proccessing policy\r\n\t" + e.getMessage() + "\r\n" + stackTrace);
		}
		finally
		{
			reportText.setText(reportTextBuilder.toString());
			IOUtils.closeQuietly(policyInput);
		}
	}
}
///CLOVER:ON
