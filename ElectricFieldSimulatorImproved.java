
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.text.DecimalFormat;
import javax.swing.Timer;

public class ElectricFieldSimulatorImproved extends JFrame {
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final double K = 8.99e9; // Coulomb constant
	private static final int FIELD_LINE_COUNT = 8; // Number of field lines per
													// charge
	private static final int FIELD_LINE_LENGTH = 100; // Length of field lines
														// in steps
	private static final double STEP_SIZE = 5.0; // Step size for field line
													// calculation
	private static final double TIME_STEP = 0.01; // Time step for particle
													// motion simulation

	private ArrayList<Charge> charges = new ArrayList<>();
	private ArrayList<TestParticle> testParticles = new ArrayList<>(); // List
																		// to
																		// hold
																		// test
																		// particles
	private JPanel controlPanel;
	private JPanel simulationPanel;
	private JTextField chargeValueField;
	private JRadioButton positiveButton;
	private JRadioButton negativeButton;
	private JCheckBox showGridCheckBox;
	private JCheckBox showVectorsCheckBox;
	private boolean showGrid = true;
	private boolean showVectors = true;

	// Particle motion controls
	private JTextField particleChargeField;
	private JRadioButton particlePositiveButton;
	private JRadioButton particleNegativeButton;
	private JTextField particleMassField;
	private JTextField particleVelocityXField;
	private JTextField particleVelocityYField;
	private JButton launchParticleButton;
	private Timer particleTimer; // Timer for particle animation

	// Charge manipulation variables
	private Charge selectedCharge = null;
	private boolean isDraggingCharge = false;

	// Force Calculation Display
	private JLabel forceMagnitudeLabel;
	private JLabel forceDirectionLabel;

	public ElectricFieldSimulatorImproved() {
		setTitle("Electric Field Simulator");
		setSize(WIDTH, HEIGHT);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		createSimulationPanel();
		createControlPanel();

		add(simulationPanel, BorderLayout.CENTER);
		add(controlPanel, BorderLayout.EAST);

		setVisible(true);

		// Timer for particle animation
		particleTimer = new Timer((int) (TIME_STEP * 1000), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateParticlePositions();
				simulationPanel.repaint();
			}
		});
	}

	private void createSimulationPanel() {
		simulationPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				// Draw grid if enabled
				if (showGrid) {
					drawGrid(g2d);
				}

				// Draw electric field vectors if enabled
				if (showVectors) {
					drawFieldVectors(g2d);
				}

				// Draw field lines
				drawFieldLines(g2d);

				// Draw charges
				for (Charge charge : charges) {
					charge.draw(g2d, selectedCharge == charge); // Highlight
																// selected
																// charge
				}

				// Draw particle trajectories and particles
				for (TestParticle particle : testParticles) {
					particle.drawTrajectory(g2d);
					particle.draw(g2d);
				}

				// Draw force vector on selected charge
				if (selectedCharge != null) {
					Vector2D netForce = calculateNetForceOnCharge(selectedCharge);
					drawForceVector(g2d, selectedCharge, netForce);
					displayForceMagnitudeDirection(netForce); // Update labels
				} else {
					clearForceDisplay(); // Clear labels if no charge selected
				}
			}
		};

		simulationPanel.setBackground(Color.WHITE);

		// Mouse Listener for adding and interacting with charges
		simulationPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) { // Left click - Add
															// charge/Select
															// Charge
					Charge clickedCharge = findChargeAt(e.getX(), e.getY());
					if (clickedCharge != null) {
						selectedCharge = clickedCharge; // Select charge if
														// clicked on
					} else if (!isDraggingCharge && selectedCharge == null) { // Add
																				// new
																				// charge
																				// if
																				// no
																				// charge
																				// clicked
																				// and
																				// not
																				// dragging
						try {
							double chargeValue = Double.parseDouble(chargeValueField.getText());
							if (negativeButton.isSelected()) {
								chargeValue = -chargeValue;
							}
							charges.add(new Charge(e.getX(), e.getY(), chargeValue));
						} catch (NumberFormatException ex) {
							JOptionPane.showMessageDialog(null, "Please enter a valid number for charge value.");
						}
					}
					simulationPanel.repaint(); // Repaint to show selection or
												// new charge
				} else if (e.getButton() == MouseEvent.BUTTON3) { // Right click
																	// -
																	// Edit/Delete
																	// Charge
					if (!isDraggingCharge && selectedCharge == null) { // Prevent
																		// editing
																		// while
																		// dragging
																		// or
																		// another
																		// charge
																		// is
																		// selected
						Charge clickedCharge = findChargeAt(e.getX(), e.getY());
						if (clickedCharge != null) {
							handleRightClickOnCharge(clickedCharge);
						}
					}
				}
				if (e.getButton() == MouseEvent.BUTTON1 && !isDraggingCharge) {
					if (findChargeAt(e.getX(), e.getY()) == null) {
						selectedCharge = null; // Deselect if clicked on
												// background
						simulationPanel.repaint();
					}
				}
				isDraggingCharge = false; // Reset drag flag after click action
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) { // Left mouse button
															// press for
															// dragging
					selectedCharge = findChargeAt(e.getX(), e.getY());
					if (selectedCharge != null) {
						isDraggingCharge = true;
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) { // Left mouse button
															// release - stop
															// dragging
					isDraggingCharge = false;
				}
			}
		});

		// Mouse Motion Listener for dragging charges
		simulationPanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (isDraggingCharge && selectedCharge != null) {
					selectedCharge.x = e.getX();
					selectedCharge.y = e.getY();
					simulationPanel.repaint();
				}
			}
		});
	}

	private Charge findChargeAt(int x, int y) {
		for (Charge charge : charges) {
			if (charge.contains(x, y)) {
				return charge;
			}
		}
		return null;
	}

	private void handleRightClickOnCharge(Charge clickedCharge) {
		String[] options = { "Edit Value", "Delete", "Cancel" };
		int choice = JOptionPane.showOptionDialog(this, "Select action for the charge:", "Charge Options",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

		if (choice == 0) { // Edit Value
			String newValueStr = JOptionPane.showInputDialog(this, "Enter new charge value (C):", clickedCharge.value);
			if (newValueStr != null) {
				try {
					double newValue = Double.parseDouble(newValueStr);
					clickedCharge.value = newValue;
					simulationPanel.repaint();
				} catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(this, "Invalid charge value entered.");
				}
			}
		} else if (choice == 1) { // Delete
			charges.remove(clickedCharge);
			selectedCharge = null; // Deselect if deleted
			simulationPanel.repaint();
		} // choice == 2 (Cancel) does nothing
	}

	private void createControlPanel() {
		controlPanel = new JPanel();
		controlPanel.setPreferredSize(new Dimension(250, HEIGHT)); // Increased
																	// width for
																	// particle
																	// controls
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// --- Charge Controls ---
		controlPanel.add(new JLabel("--- Charge Settings ---"));

		// Charge value input
		JPanel chargePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		chargePanel.add(new JLabel("Charge Value (C):"));
		controlPanel.add(chargePanel);

		chargeValueField = new JTextField("1.0e-9", 10);
		JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		fieldPanel.add(chargeValueField);
		controlPanel.add(fieldPanel);

		// Radio buttons for charge sign
		ButtonGroup chargeGroup = new ButtonGroup();
		positiveButton = new JRadioButton("Positive", true);
		negativeButton = new JRadioButton("Negative");
		chargeGroup.add(positiveButton);
		chargeGroup.add(negativeButton);

		JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		radioPanel.add(positiveButton);
		radioPanel.add(negativeButton);
		controlPanel.add(radioPanel);

		// --- Display Options ---
		controlPanel.add(Box.createVerticalStrut(10));
		controlPanel.add(new JLabel("--- Display Options ---"));

		// Checkboxes for display options
		showGridCheckBox = new JCheckBox("Show Grid", showGrid);
		showGridCheckBox.addActionListener(e -> {
			showGrid = showGridCheckBox.isSelected();
			simulationPanel.repaint();
		});

		showVectorsCheckBox = new JCheckBox("Show Field Vectors", showVectors);
		showVectorsCheckBox.addActionListener(e -> {
			showVectors = showVectorsCheckBox.isSelected();
			simulationPanel.repaint();
		});

		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
		checkBoxPanel.add(showGridCheckBox);
		checkBoxPanel.add(showVectorsCheckBox);
		controlPanel.add(checkBoxPanel);

		// --- Clear Button ---
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton clearButton = new JButton("Clear All Charges");
		clearButton.addActionListener(e -> {
			charges.clear();
			selectedCharge = null; // Deselect on clear all
			simulationPanel.repaint();
		});
		buttonPanel.add(clearButton);
		controlPanel.add(buttonPanel);

		// --- Force Display ---
		controlPanel.add(Box.createVerticalStrut(20));
		controlPanel.add(new JLabel("--- Force on Selected Charge ---"));
		forceMagnitudeLabel = new JLabel("Magnitude: N/A");
		forceDirectionLabel = new JLabel("Direction: N/A");
		controlPanel.add(forceMagnitudeLabel);
		controlPanel.add(forceDirectionLabel);

		// --- Particle Motion Controls ---
		controlPanel.add(Box.createVerticalStrut(20));
		controlPanel.add(new JLabel("--- Particle Motion ---"));

		// Particle charge input
		JPanel particleChargePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		particleChargePanel.add(new JLabel("Particle Charge (C):"));
		controlPanel.add(particleChargePanel);
		particleChargeField = new JTextField("1.0e-10", 10);
		controlPanel.add(new JPanel(new FlowLayout(FlowLayout.LEFT)).add(particleChargeField));

		// Particle charge sign
		ButtonGroup particleChargeGroup = new ButtonGroup();
		particlePositiveButton = new JRadioButton("Positive", true);
		particleNegativeButton = new JRadioButton("Negative");
		particleChargeGroup.add(particlePositiveButton);
		particleChargeGroup.add(particleNegativeButton);
		JPanel particleRadioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		particleRadioPanel.add(particlePositiveButton);
		particleRadioPanel.add(particleNegativeButton);
		controlPanel.add(particleRadioPanel);

		// Particle mass input
		JPanel particleMassPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		particleMassPanel.add(new JLabel("Particle Mass (kg):"));
		controlPanel.add(particleMassPanel);
		particleMassField = new JTextField("1.0e-15", 10);
		controlPanel.add(new JPanel(new FlowLayout(FlowLayout.LEFT)).add(particleMassField));

		// Initial velocity input
		JPanel velocityXPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		velocityXPanel.add(new JLabel("Velocity X (m/s):"));
		controlPanel.add(velocityXPanel);
		particleVelocityXField = new JTextField("0", 5);
		controlPanel.add(new JPanel(new FlowLayout(FlowLayout.LEFT)).add(particleVelocityXField));

		JPanel velocityYPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		velocityYPanel.add(new JLabel("Velocity Y (m/s):"));
		controlPanel.add(velocityYPanel);
		particleVelocityYField = new JTextField("0", 5);
		controlPanel.add(new JPanel(new FlowLayout(FlowLayout.LEFT)).add(particleVelocityYField));

		// Launch particle button
		JPanel launchButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		launchParticleButton = new JButton("Launch Particle");
		launchParticleButton.addActionListener(e -> launchTestParticle());
		launchButtonPanel.add(launchParticleButton);
		controlPanel.add(launchButtonPanel);

		JButton clearParticlesButton = new JButton("Clear Particles");
		clearParticlesButton.addActionListener(e -> {
			testParticles.clear();
			simulationPanel.repaint();
			particleTimer.stop(); // Stop timer when particles are cleared.
		});
		JPanel clearParticlesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		clearParticlesPanel.add(clearParticlesButton);
		controlPanel.add(clearParticlesPanel);

		// Instructions
		JTextArea instructionsArea = new JTextArea("Instructions:\n\n" + "1. Left-click to add/select\n   charges.\n"
				+ "2. Drag charges to\n   reposition.\n" + "3. Right-click charges\n   to edit/delete.\n"
				+ "4. Select a charge to\n   see net force.\n" + "5. Set particle params\n   and launch.\n"
				+ "6. Toggle display\n   options as needed.");
		instructionsArea.setEditable(false);
		instructionsArea.setBackground(controlPanel.getBackground());
		controlPanel.add(Box.createVerticalStrut(20));
		controlPanel.add(instructionsArea);

		// Add filler to push everything to the top
		controlPanel.add(Box.createVerticalGlue());
	}

	private void launchTestParticle() {
		try {
			double particleChargeValue = Double.parseDouble(particleChargeField.getText());
			if (particleNegativeButton.isSelected()) {
				particleChargeValue = -particleChargeValue;
			}
			double particleMass = Double.parseDouble(particleMassField.getText());
			double particleVelocityX = Double.parseDouble(particleVelocityXField.getText());
			double particleVelocityY = Double.parseDouble(particleVelocityYField.getText());

			TestParticle particle = new TestParticle(WIDTH / 2.0, HEIGHT / 2.0, particleChargeValue, particleMass,
					particleVelocityX, particleVelocityY); // Start at center
			testParticles.add(particle);
			if (!particleTimer.isRunning()) {
				particleTimer.start(); // Start timer only when particles are
										// launched
			}
			simulationPanel.repaint();

		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(null, "Please enter valid numbers for particle parameters.");
		}
	}

	private void updateParticlePositions() {
		for (TestParticle particle : testParticles) {
			Vector2D field = calculateElectricField(particle.x, particle.y);
			Vector2D force = field.multiply(particle.charge);
			Vector2D acceleration = force.multiply(1.0 / particle.mass);

			particle.vx += acceleration.x * TIME_STEP;
			particle.vy += acceleration.y * TIME_STEP;

			particle.x += particle.vx * TIME_STEP;
			particle.y += particle.vy * TIME_STEP;

			particle.trajectory.add(new Point2D.Double(particle.x, particle.y)); // Add
																					// to
																					// trajectory

			// Basic boundary collision (particles bounce off walls)
			if (particle.x < 0 || particle.x > simulationPanel.getWidth()) {
				particle.vx *= -0.8; // Reverse and dampen velocity
				particle.x = Math.max(0, Math.min(particle.x, simulationPanel.getWidth())); // Keep
																							// within
																							// bounds
			}
			if (particle.y < 0 || particle.y > simulationPanel.getHeight()) {
				particle.vy *= -0.8; // Reverse and dampen velocity
				particle.y = Math.max(0, Math.min(particle.y, simulationPanel.getHeight())); // Keep
																								// within
																								// bounds
			}
		}
	}

	private void drawGrid(Graphics2D g2d) {
		g2d.setColor(new Color(220, 220, 220));

		// Draw horizontal grid lines
		for (int y = 0; y < HEIGHT; y += 50) {
			g2d.drawLine(0, y, simulationPanel.getWidth(), y);
		}

		// Draw vertical grid lines
		for (int x = 0; x < WIDTH; x += 50) {
			g2d.drawLine(x, 0, x, simulationPanel.getHeight());
		}
	}

	private void drawFieldVectors(Graphics2D g2d) {
		int spacing = 40; // Spacing between field vectors

		for (int x = spacing; x < simulationPanel.getWidth(); x += spacing) {
			for (int y = spacing; y < simulationPanel.getHeight(); y += spacing) {
				// Skip drawing vectors too close to charges
				boolean tooClose = false;
				for (Charge charge : charges) {
					double dist = distance(x, y, charge.x, charge.y);
					if (dist < 20) {
						tooClose = true;
						break;
					}
				}

				if (!tooClose) {
					Vector2D field = calculateElectricField(x, y);
					if (field.magnitude() > 0) {
						drawArrow(g2d, x, y, field);
					}
				}
			}
		}
	}

	private void drawFieldLines(Graphics2D g2d) {
		for (Charge charge : charges) {
			// Draw field lines only for charges with non-zero magnitude
			if (Math.abs(charge.value) > 0) {
				int numLines = FIELD_LINE_COUNT;
				double angleStep = 2 * Math.PI / numLines;

				for (int i = 0; i < numLines; i++) {
					double angle = i * angleStep;
					double startX = charge.x + 15 * Math.cos(angle);
					double startY = charge.y + 15 * Math.sin(angle);

					drawFieldLine(g2d, startX, startY, charge.value > 0);
				}
			}
		}
	}

	private void drawFieldLine(Graphics2D g2d, double startX, double startY, boolean outward) {
		Path2D path = new Path2D.Double();
		path.moveTo(startX, startY);

		double x = startX;
		double y = startY;

		g2d.setColor(new Color(0, 0, 200, 150));
		g2d.setStroke(new BasicStroke(1.5f));

		for (int i = 0; i < FIELD_LINE_LENGTH; i++) {
			Vector2D field = calculateElectricField(x, y);

			if (field.magnitude() < 1e-10) {
				break; // Stop if field is too weak
			}

			// Normalize and scale field vector
			field = field.normalize().multiply(STEP_SIZE);

			// Reverse direction for inward field lines (negative charges)
			if (!outward) {
				field = field.multiply(-1);
			}

			// Update position
			x += field.x;
			y += field.y;

			// Check if we're out of bounds
			if (x < 0 || x > simulationPanel.getWidth() || y < 0 || y > simulationPanel.getHeight()) {
				break;
			}

			// Check if we're too close to any charge (other than starting
			// charge)
			boolean tooClose = false;
			for (Charge charge : charges) {
				double dist = distance(x, y, charge.x, charge.y);
				if (dist < 10) {
					tooClose = true;
					break;
				}
			}

			if (tooClose) {
				break;
			}

			path.lineTo(x, y);
		}

		g2d.draw(path);
	}

	private void drawArrow(Graphics2D g2d, double x, double y, Vector2D vector) {
		double scaleFactor = 1000000000000.0; // Scale factor to make vectors
												// visible
		double magnitude = vector.magnitude();

		if (magnitude < 1e-12) {
			return; // Don't draw very small vectors
		}

		// Scale magnitude logarithmically for better visualization
		double logScale = Math.log10(magnitude * scaleFactor) * 5;
		if (logScale < 5)
			logScale = 5;
		if (logScale > 25)
			logScale = 25;

		Vector2D normalized = vector.normalize().multiply(logScale);

		double endX = x + normalized.x;
		double endY = y + normalized.y;

		// Determine color based on field strength
		int colorIntensity = (int) Math.min(255, Math.max(0, (magnitude * scaleFactor) * 100));
		g2d.setColor(new Color(colorIntensity, 0, 255 - colorIntensity));

		// Draw line
		g2d.setStroke(new BasicStroke(1.0f));
		g2d.draw(new Line2D.Double(x, y, endX, endY));

		// Draw arrowhead
		double arrowLength = 5;
		double arrowAngle = Math.atan2(normalized.y, normalized.x);
		double arrowAngle1 = arrowAngle - Math.PI / 6;
		double arrowAngle2 = arrowAngle + Math.PI / 6;

		double arrowX1 = endX - arrowLength * Math.cos(arrowAngle1);
		double arrowY1 = endY - arrowLength * Math.sin(arrowAngle1);
		double arrowX2 = endX - arrowLength * Math.cos(arrowAngle2);
		double arrowY2 = endY - arrowLength * Math.sin(arrowAngle2);

		g2d.draw(new Line2D.Double(endX, endY, arrowX1, arrowY1));
		g2d.draw(new Line2D.Double(endX, endY, arrowX2, arrowY2));
	}

	private Vector2D calculateElectricField(double x, double y) {
		Vector2D totalField = new Vector2D(0, 0);

		for (Charge charge : charges) {
			double dx = x - charge.x;
			double dy = y - charge.y;
			double distSquared = dx * dx + dy * dy;

			// Avoid division by zero or very small values
			if (distSquared < 1) {
				distSquared = 1;
			}

			double magnitude = K * Math.abs(charge.value) / distSquared;
			double direction = (charge.value > 0) ? 1 : -1;

			double dist = Math.sqrt(distSquared);
			Vector2D fieldVector = new Vector2D(dx / dist, dy / dist);
			fieldVector = fieldVector.multiply(direction * magnitude);

			totalField = totalField.add(fieldVector);
		}

		return totalField;
	}

	// New method to calculate net force on a charge
	private Vector2D calculateNetForceOnCharge(Charge targetCharge) {
		Vector2D netForce = new Vector2D(0, 0);
		for (Charge otherCharge : charges) {
			if (otherCharge != targetCharge) { // Don't calculate force of
												// charge on itself
				double dx = targetCharge.x - otherCharge.x;
				double dy = targetCharge.y - otherCharge.y;
				double distSquared = dx * dx + dy * dy;

				if (distSquared < 1)
					distSquared = 1; // Prevent very large forces at close
										// distances

				double forceMagnitude = K * Math.abs(targetCharge.value * otherCharge.value) / distSquared;
				double forceDirectionSign = (targetCharge.value * otherCharge.value > 0) ? -1 : 1; // Repel
																									// if
																									// same
																									// sign,
																									// attract
																									// if
																									// opposite

				double dist = Math.sqrt(distSquared);
				Vector2D forceVector = new Vector2D(dx / dist, dy / dist); // Vector
																			// pointing
																			// from
																			// otherCharge
																			// to
																			// targetCharge
				forceVector = forceVector.normalize().multiply(forceDirectionSign * forceMagnitude); // Scale
																										// by
																										// magnitude
																										// and
																										// direction

				netForce = netForce.add(forceVector);
			}
		}
		return netForce;
	}

	private void drawForceVector(Graphics2D g2d, Charge charge, Vector2D force) {
		double scaleFactor = 5e9; // Adjust scale factor as needed to visualize
									// force vector
		Vector2D scaledForce = force.multiply(scaleFactor);

		double startX = charge.x;
		double startY = charge.y;
		double endX = startX + scaledForce.x;
		double endY = startY + scaledForce.y;

		g2d.setColor(new Color(0, 200, 0)); // Green color for force vector
		g2d.setStroke(new BasicStroke(2.0f)); // Thicker stroke for force vector
		g2d.draw(new Line2D.Double(startX, startY, endX, endY));

		// Arrowhead (similar to field vector arrowhead)
		double arrowLength = 8;
		double arrowAngle = Math.atan2(scaledForce.y, scaledForce.x);
		double arrowAngle1 = arrowAngle - Math.PI / 6;
		double arrowAngle2 = arrowAngle + Math.PI / 6;

		double arrowX1 = endX - arrowLength * Math.cos(arrowAngle1);
		double arrowY1 = endY - arrowLength * Math.sin(arrowAngle1);
		double arrowX2 = endX - arrowLength * Math.cos(arrowAngle2);
		double arrowY2 = endY - arrowLength * Math.sin(arrowAngle2);

		g2d.draw(new Line2D.Double(endX, endY, arrowX1, arrowY1));
		g2d.draw(new Line2D.Double(endX, endY, arrowX2, arrowY2));
	}

	private void displayForceMagnitudeDirection(Vector2D force) {
		DecimalFormat df = new DecimalFormat("0.##E0");
		double magnitude = force.magnitude();
		double directionDegrees = Math.toDegrees(Math.atan2(force.y, force.x));
		if (directionDegrees < 0)
			directionDegrees += 360; // Ensure angle is 0-360

		forceMagnitudeLabel.setText("Magnitude: " + df.format(magnitude) + " N");
		forceDirectionLabel.setText("Direction: " + df.format(directionDegrees) + "°");
	}

	private void clearForceDisplay() {
		forceMagnitudeLabel.setText("Magnitude: N/A");
		forceDirectionLabel.setText("Direction: N/A");
	}

	private double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new ElectricFieldSimulatorImproved());
	}

	// Class to represent a charge
	class Charge {
		double x, y;
		double value; // in Coulombs
		private static final int RADIUS = 12; // Define radius as a constant

		public Charge(double x, double y, double value) {
			this.x = x;
			this.y = y;
			this.value = value;
		}

		public void draw(Graphics2D g2d, boolean isSelected) {
			Color color = (value > 0) ? new Color(255, 0, 0, 200) : new Color(0, 0, 255, 200);

			g2d.setColor(color);
			g2d.fill(new Ellipse2D.Double(x - RADIUS, y - RADIUS, 2 * RADIUS, 2 * RADIUS));

			if (isSelected) { // Highlight selected charge
				g2d.setColor(Color.YELLOW); // Or any highlight color
				g2d.setStroke(new BasicStroke(3)); // Thicker stroke for
													// highlight
				g2d.draw(new Ellipse2D.Double(x - RADIUS - 2, y - RADIUS - 2, 2 * RADIUS + 4, 2 * RADIUS + 4));
				g2d.setStroke(new BasicStroke(1)); // Reset stroke
			}

			g2d.setColor(Color.WHITE);
			String sign = (value > 0) ? "+" : "-";
			FontMetrics fm = g2d.getFontMetrics();
			int textWidth = fm.stringWidth(sign);
			int textHeight = fm.getHeight();
			g2d.drawString(sign, (float) (x - textWidth / 2), (float) (y + textHeight / 4));

			// Draw charge value as text
			DecimalFormat df = new DecimalFormat("0.##E0");
			String valueText = df.format(value);
			g2d.setColor(Color.BLACK);
			g2d.drawString(valueText, (float) (x + RADIUS + 2), (float) (y + RADIUS));
		}

		public void draw(Graphics2D g2d) {
			draw(g2d, false); // Default draw without selection highlight
		}

		public boolean contains(double px, double py) {
			return distance(x, y, px, py) <= RADIUS;
		}

		private double distance(double x1, double y1, double x2, double y2) {
			return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
		}
	}

	// Class to represent a 2D vector
	class Vector2D {
		double x, y;

		public Vector2D(double x, double y) {
			this.x = x;
			this.y = y;
		}

		public double magnitude() {
			return Math.sqrt(x * x + y * y);
		}

		public Vector2D normalize() {
			double mag = magnitude();
			if (mag > 0) {
				return new Vector2D(x / mag, y / mag);
			} else {
				return new Vector2D(0, 0);
			}
		}

		public Vector2D add(Vector2D other) {
			return new Vector2D(this.x + other.x, this.y + other.y);
		}

		public Vector2D multiply(double scalar) {
			return new Vector2D(this.x * scalar, this.y * scalar);
		}
	}

	// Class to represent a test particle
	class TestParticle {
		double x, y;
		double vx, vy;
		double charge;
		double mass;
		ArrayList<Point2D.Double> trajectory = new ArrayList<>();

		public TestParticle(double x, double y, double charge, double mass, double vx, double vy) {
			this.x = x;
			this.y = y;
			this.charge = charge;
			this.mass = mass;
			this.vx = vx;
			this.vy = vy;
			trajectory.add(new Point2D.Double(x, y)); // Add initial position to
														// trajectory
		}

		public void draw(Graphics2D g2d) {
			int radius = 6;
			Color color = (charge > 0) ? new Color(255, 100, 100, 200) : new Color(100, 100, 255, 200); // Lighter
																										// colors
																										// for
																										// particles

			g2d.setColor(color);
			g2d.fill(new Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius));
		}

		public void drawTrajectory(Graphics2D g2d) {
			g2d.setColor(new Color(150, 150, 150, 100)); // Light gray for
															// trajectory
			g2d.setStroke(new BasicStroke(0.5f));
			if (trajectory.size() > 1) {
				for (int i = 0; i < trajectory.size() - 1; i++) {
					Point2D.Double p1 = trajectory.get(i);
					Point2D.Double p2 = trajectory.get(i + 1);
					g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
				}
			}
		}
	}
}
