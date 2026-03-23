package br.eti.ljr.qrcode;

import java.awt.EventQueue;

import br.eti.ljr.qrcode.ui.UiPrincipal;

public class QrCode {

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UiPrincipal window = new UiPrincipal();
					window.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace(); 
				}
			}
		});
	}
}
