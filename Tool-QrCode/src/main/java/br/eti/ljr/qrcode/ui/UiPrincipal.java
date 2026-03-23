package br.eti.ljr.qrcode.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Hashtable;

import javax.imageio.ImageIO;
//import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.MaskFormatter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javax.swing.border.TitledBorder;

public class UiPrincipal {

	private static final String formatoQrCodeGerado = "png";

	private JFrame frame;
	private JTextField txfLink;
	private JTextField txfTamanho;
	private JTextField txfNomeArquivo;
 
	private JTextField txtCaminho;
	private JButton btnSelecionarCaminho;
	private JLabel lblNewLabel_3;
	private JTextPane txtMensagem;

	private JLabel lblLogo;

// ...

	private JFormattedTextField txtTelefone;

	/**
	 * Create the application.
	 * 
	 * @throws ParseException
	 */
	public UiPrincipal() throws ParseException {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 * 
	 * @throws ParseException
	 */
	private static final Image ICONE_SISTEMA = carregarImagem("icon.png");

	private static Image carregarImagem(String nome) {
		try (var input = UiPrincipal.class.getResourceAsStream("/" + nome)) { // ← adicione a barra!
			if (input == null) {
				System.err.println("Imagem não encontrada na raiz do classpath: /" + nome);
				return null;
			}
			return ImageIO.read(input);
		} catch (IOException e) {
			System.err.println("Erro ao carregar imagem: /" + nome);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Método único que valida TODOS os campos do formulário.
	 * 
	 * @param paraGerarQR true = validação mais rigorosa (inclui link, tamanho,
	 *                    pasta, etc.) false = validação apenas para montar o link
	 *                    (telefone, país, mensagem)
	 * @return true se tudo está válido, false se houver erro (e já mostra mensagem
	 *         para o usuário)
	 */
	
	private boolean validarTudo(boolean paraGerarQR) {

		// 2. Telefone preenchido e com dígitos razoáveis?
		String telefoneRaw = txtTelefone.getText().trim();
		String telefoneLimpo = telefoneRaw.replaceAll("[^0-9]", "");

		if (telefoneLimpo.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "O número de telefone é obrigatório!", "Campo obrigatório",
					JOptionPane.WARNING_MESSAGE);
			txtTelefone.requestFocus();
			return false;
		}

		if (telefoneLimpo.length() < 8 || telefoneLimpo.length() > 15) {
			JOptionPane.showMessageDialog(frame,
					"Número de telefone inválido!\nDeve ter entre 8 e 15 dígitos (sem DDI).", "Telefone inválido",
					JOptionPane.WARNING_MESSAGE);
			txtTelefone.requestFocus();
			return false;
		}

		// 3. Mensagem (opcional – avisa se vazia)
		String mensagem = txtMensagem.getText().trim();
		if (mensagem.isEmpty()) {
			int resposta = JOptionPane.showConfirmDialog(frame,
					"A mensagem está vazia.\nContinuar sem texto pré-preenchido no WhatsApp?", "Mensagem vazia",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (resposta != JOptionPane.YES_OPTION) {
				txtMensagem.requestFocus();
				return false;
			}
		}

		// Validações ADICIONAIS quando for gerar o QR Code
		if (paraGerarQR) {

			// 4. Link já foi montado?
			String link = txfLink.getText().trim();
			if (link.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Você precisa montar o link primeiro!\nClique em 'Montar link'.",
						"Link não gerado", JOptionPane.WARNING_MESSAGE);
				return false;
			}

			// 5. Tamanho do QR válido?
			String tamStr = txfTamanho.getText().trim();
			try {
				int tamanho = Integer.parseInt(tamStr);
				if (tamanho < 100 || tamanho > 2000) {
					JOptionPane.showMessageDialog(frame,
							"Tamanho do QR Code inválido!\nUse um valor entre 100 e 2000 pixels.", "Tamanho inválido",
							JOptionPane.WARNING_MESSAGE);
					txfTamanho.requestFocus();
					return false;
				}
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(frame, "O tamanho do QR Code deve ser um número inteiro!",
						"Formato inválido", JOptionPane.ERROR_MESSAGE);
				txfTamanho.requestFocus();
				return false;
			}

			// 6. Nome do arquivo preenchido?
			String nomeArquivo = txfNomeArquivo.getText().trim();
			if (nomeArquivo.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Digite um nome para o arquivo!", "Campo obrigatório",
						JOptionPane.WARNING_MESSAGE);
				txfNomeArquivo.requestFocus();
				return false;
			}

			// 7. Pasta de salvamento válida?
			String caminho = txtCaminho.getText().trim();
			if (caminho.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Selecione uma pasta para salvar!", "Campo obrigatório",
						JOptionPane.WARNING_MESSAGE);
				txtCaminho.requestFocus();
				return false;
			}

			File pasta = new File(caminho);
			if (!pasta.exists()) {
				JOptionPane.showMessageDialog(frame, "A pasta selecionada não existe!", "Pasta inválida",
						JOptionPane.ERROR_MESSAGE);
				txtCaminho.requestFocus();
				return false;
			}
			if (!pasta.isDirectory()) {
				JOptionPane.showMessageDialog(frame, "O caminho selecionado não é uma pasta!", "Caminho inválido",
						JOptionPane.ERROR_MESSAGE);
				txtCaminho.requestFocus();
				return false;
			}
			if (!pasta.canWrite()) {
				JOptionPane.showMessageDialog(frame, "Sem permissão para escrever na pasta!", "Permissão negada",
						JOptionPane.ERROR_MESSAGE);
				txtCaminho.requestFocus();
				return false;
			}
		}

		// Tudo validado com sucesso
		return true;
	}

	private void initialize() throws ParseException {
		frame = new JFrame();
		if (ICONE_SISTEMA != null) {
		    frame.setIconImage(ICONE_SISTEMA);
		}
		frame.setResizable(false);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Gerar QRCode");
		frame.setBounds(100, 100, 523, 556);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		txfLink = new JTextField();
		txfLink.setBounds(10, 288, 490, 28);
		frame.getContentPane().add(txfLink);
		txfLink.setColumns(10);

		JLabel lblNewLabel = new JLabel("Link");
		lblNewLabel.setBounds(10, 267, 46, 14);
		frame.getContentPane().add(lblNewLabel);

		lblLogo = new JLabel();
//		if (ICONE_SISTEMA != null) {
//			lblLogo.setIcon(new ImageIcon(ICONE_SISTEMA));
//		}
		lblLogo.setBounds(473, 12, 17, 10);
		frame.getContentPane().add(lblLogo);

		lblLogo.setBounds(410, 10, 80, 80);
		frame.getContentPane().add(lblLogo);

		JButton btnGerar = new JButton("Gerar QRcode");
		btnGerar.addActionListener(e -> {
			if (txfLink.getText() == null || txfLink.getText().isEmpty()) {
				JOptionPane.showMessageDialog(frame, "O link a ser codificado é obrigatório");
			} else {

				int tamanho = Integer.parseInt(txfTamanho.getText());
				String path = txtCaminho.getText();
				gerarComZXing(path, txfLink.getText(), txfNomeArquivo.getText(), tamanho);
			}
		});
		btnGerar.setBounds(290, 478, 210, 28);
		frame.getContentPane().add(btnGerar);

		txfTamanho = new JTextField();
		txfTamanho.setText("200");
		txfTamanho.setBounds(388, 363, 112, 28);
		frame.getContentPane().add(txfTamanho);
		txfTamanho.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("Tamanho do qrcode");
		lblNewLabel_1.setBounds(388, 344, 158, 14);
		frame.getContentPane().add(lblNewLabel_1);

		txfNomeArquivo = new JTextField();
		txfNomeArquivo.setText("qrcode-16-03-2026");
		txfNomeArquivo.setBounds(10, 363, 366, 28);
		frame.getContentPane().add(txfNomeArquivo);
		txfNomeArquivo.setColumns(10);

		JLabel lblNewLabel_2 = new JLabel("Nome arquivo");
		lblNewLabel_2.setBounds(10, 344, 154, 14);
		frame.getContentPane().add(lblNewLabel_2);

		JComboBox<String> comboPais = new JComboBox<>();
		comboPais.addItem("+93 Afeganistão");
		comboPais.addItem("+355 Albânia");
		comboPais.addItem("+213 Argélia");
		comboPais.addItem("+376 Andorra");
		comboPais.addItem("+244 Angola");
		comboPais.addItem("+1 Anguila");
		comboPais.addItem("+1 Antígua e Barbuda");
		comboPais.addItem("+54 Argentina");
		comboPais.addItem("+374 Armênia");
		comboPais.addItem("+61 Austrália");
		comboPais.addItem("+43 Áustria");
		comboPais.addItem("+994 Azerbaijão");
		comboPais.addItem("+1 Bahamas");
		comboPais.addItem("+973 Bahrein");
		comboPais.addItem("+880 Bangladesh");
		comboPais.addItem("+1 Barbados");
		comboPais.addItem("+375 Belarus");
		comboPais.addItem("+32 Bélgica");
		comboPais.addItem("+501 Belize");
		comboPais.addItem("+229 Benim");
		comboPais.addItem("+975 Butão");
		comboPais.addItem("+591 Bolívia");
		comboPais.addItem("+387 Bósnia e Herzegovina");
		comboPais.addItem("+267 Botsuana");
		comboPais.addItem("+55 Brasil"); // ← este é o que você quer selecionado
		comboPais.addItem("+673 Brunei");
		comboPais.addItem("+359 Bulgária");
		comboPais.addItem("+226 Burkina Faso");
		comboPais.addItem("+257 Burundi");
		comboPais.addItem("+855 Camboja");
		comboPais.addItem("+237 Camarões");
		comboPais.addItem("+1 Canadá");
		comboPais.addItem("+238 Cabo Verde");
		comboPais.addItem("+1 Ilhas Cayman");
		comboPais.addItem("+236 República Centro-Africana");
		comboPais.addItem("+235 Chade");
		comboPais.addItem("+56 Chile");
		comboPais.addItem("+86 China");
		comboPais.addItem("+57 Colômbia");
		comboPais.addItem("+269 Comores");
		comboPais.addItem("+242 Congo");
		comboPais.addItem("+243 República Democrática do Congo");
		comboPais.addItem("+682 Ilhas Cook");
		comboPais.addItem("+506 Costa Rica");
		comboPais.addItem("+385 Croácia");
		comboPais.addItem("+53 Cuba");
		comboPais.addItem("+357 Chipre");
		comboPais.addItem("+420 República Tcheca");
		comboPais.addItem("+45 Dinamarca");
		comboPais.addItem("+253 Djibouti");
		comboPais.addItem("+1 Dominica");
		comboPais.addItem("+1 República Dominicana");
		comboPais.addItem("+593 Equador");
		comboPais.addItem("+20 Egito");
		comboPais.addItem("+503 El Salvador");
		comboPais.addItem("+240 Guiné Equatorial");
		comboPais.addItem("+291 Eritreia");
		comboPais.addItem("+372 Estônia");
		comboPais.addItem("+268 Essuatíni");
		comboPais.addItem("+251 Etiópia");
		comboPais.addItem("+500 Ilhas Malvinas");
		comboPais.addItem("+298 Ilhas Faroé");
		comboPais.addItem("+679 Fiji");
		comboPais.addItem("+358 Finlândia");
		comboPais.addItem("+33 França");
		comboPais.addItem("+594 Guiana Francesa");
		comboPais.addItem("+689 Polinésia Francesa");
		comboPais.addItem("+241 Gabão");
		comboPais.addItem("+220 Gâmbia");
		comboPais.addItem("+995 Geórgia");
		comboPais.addItem("+49 Alemanha");
		comboPais.addItem("+233 Gana");
		comboPais.addItem("+350 Gibraltar");
		comboPais.addItem("+30 Grécia");
		comboPais.addItem("+299 Groenlândia");
		comboPais.addItem("+1 Granada");
		comboPais.addItem("+590 Guadalupe");
		comboPais.addItem("+1 Guam");
		comboPais.addItem("+502 Guatemala");
		comboPais.addItem("+224 Guiné");
		comboPais.addItem("+245 Guiné-Bissau");
		comboPais.addItem("+592 Guiana");
		comboPais.addItem("+509 Haiti");
		comboPais.addItem("+504 Honduras");
		comboPais.addItem("+852 Hong Kong");
		comboPais.addItem("+36 Hungria");
		comboPais.addItem("+354 Islândia");
		comboPais.addItem("+91 Índia");
		comboPais.addItem("+62 Indonésia");
		comboPais.addItem("+98 Irã");
		comboPais.addItem("+964 Iraque");
		comboPais.addItem("+353 Irlanda");
		comboPais.addItem("+972 Israel");
		comboPais.addItem("+39 Itália");
		comboPais.addItem("+1 Jamaica");
		comboPais.addItem("+81 Japão");
		comboPais.addItem("+962 Jordânia");
		comboPais.addItem("+7 Cazaquistão");
		comboPais.addItem("+254 Quênia");
		comboPais.addItem("+686 Kiribati");
		comboPais.addItem("+850 Coreia do Norte");
		comboPais.addItem("+82 Coreia do Sul");
		comboPais.addItem("+965 Kuwait");
		comboPais.addItem("+996 Quirguistão");
		comboPais.addItem("+856 Laos");
		comboPais.addItem("+371 Letônia");
		comboPais.addItem("+961 Líbano");
		comboPais.addItem("+266 Lesoto");
		comboPais.addItem("+231 Libéria");
		comboPais.addItem("+218 Líbia");
		comboPais.addItem("+423 Liechtenstein");
		comboPais.addItem("+370 Lituânia");
		comboPais.addItem("+352 Luxemburgo");
		comboPais.addItem("+853 Macau");
		comboPais.addItem("+389 Macedônia do Norte");
		comboPais.addItem("+261 Madagascar");
		comboPais.addItem("+265 Malawi");
		comboPais.addItem("+60 Malásia");
		comboPais.addItem("+960 Maldivas");
		comboPais.addItem("+223 Mali");
		comboPais.addItem("+356 Malta");
		comboPais.addItem("+692 Ilhas Marshall");
		comboPais.addItem("+596 Martinica");
		comboPais.addItem("+222 Mauritânia");
		comboPais.addItem("+230 Maurício");
		comboPais.addItem("+52 México");
		comboPais.addItem("+691 Micronésia");
		comboPais.addItem("+373 Moldávia");
		comboPais.addItem("+377 Mônaco");
		comboPais.addItem("+976 Mongólia");
		comboPais.addItem("+382 Montenegro");
		comboPais.addItem("+212 Marrocos");
		comboPais.addItem("+258 Moçambique");
		comboPais.addItem("+95 Mianmar");
		comboPais.addItem("+264 Namíbia");
		comboPais.addItem("+674 Nauru");
		comboPais.addItem("+977 Nepal");
		comboPais.addItem("+31 Países Baixos");
		comboPais.addItem("+64 Nova Zelândia");
		comboPais.addItem("+505 Nicarágua");
		comboPais.addItem("+227 Níger");
		comboPais.addItem("+234 Nigéria");
		comboPais.addItem("+47 Noruega");
		comboPais.addItem("+968 Omã");
		comboPais.addItem("+92 Paquistão");
		comboPais.addItem("+680 Palau");
		comboPais.addItem("+507 Panamá");
		comboPais.addItem("+675 Papua-Nova Guiné");
		comboPais.addItem("+595 Paraguai");
		comboPais.addItem("+51 Peru");
		comboPais.addItem("+63 Filipinas");
		comboPais.addItem("+48 Polônia");
		comboPais.addItem("+351 Portugal");
		comboPais.addItem("+1 Porto Rico");
		comboPais.addItem("+974 Catar");
		comboPais.addItem("+40 Romênia");
		comboPais.addItem("+7 Rússia");
		comboPais.addItem("+250 Ruanda");
		comboPais.addItem("+1 São Cristóvão e Névis");
		comboPais.addItem("+1 Santa Lúcia");
		comboPais.addItem("+1 São Vicente e Granadinas");
		comboPais.addItem("+685 Samoa");
		comboPais.addItem("+378 San Marino");
		comboPais.addItem("+966 Arábia Saudita");
		comboPais.addItem("+221 Senegal");
		comboPais.addItem("+381 Sérvia");
		comboPais.addItem("+248 Seychelles");
		comboPais.addItem("+232 Serra Leoa");
		comboPais.addItem("+65 Singapura");
		comboPais.addItem("+421 Eslováquia");
		comboPais.addItem("+386 Eslovênia");
		comboPais.addItem("+677 Ilhas Salomão");
		comboPais.addItem("+252 Somália");
		comboPais.addItem("+27 África do Sul");
		comboPais.addItem("+211 Sudão do Sul");
		comboPais.addItem("+34 Espanha");
		comboPais.addItem("+94 Sri Lanka");
		comboPais.addItem("+249 Sudão");
		comboPais.addItem("+597 Suriname");
		comboPais.addItem("+46 Suécia");
		comboPais.addItem("+41 Suíça");
		comboPais.addItem("+963 Síria");
		comboPais.addItem("+886 Taiwan");
		comboPais.addItem("+992 Tajiquistão");
		comboPais.addItem("+255 Tanzânia");
		comboPais.addItem("+66 Tailândia");
		comboPais.addItem("+670 Timor-Leste");
		comboPais.addItem("+228 Togo");
		comboPais.addItem("+676 Tonga");
		comboPais.addItem("+1 Trinidad e Tobago");
		comboPais.addItem("+216 Tunísia");
		comboPais.addItem("+90 Turquia");
		comboPais.addItem("+993 Turcomenistão");
		comboPais.addItem("+688 Tuvalu");
		comboPais.addItem("+256 Uganda");
		comboPais.addItem("+380 Ucrânia");
		comboPais.addItem("+971 Emirados Árabes Unidos");
		comboPais.addItem("+44 Reino Unido");
		comboPais.addItem("+1 Estados Unidos");
		comboPais.addItem("+598 Uruguai");
		comboPais.addItem("+998 Uzbequistão");
		comboPais.addItem("+678 Vanuatu");
		comboPais.addItem("+379 Vaticano");
		comboPais.addItem("+58 Venezuela");
		comboPais.addItem("+84 Vietnã");
		comboPais.addItem("+967 Iêmen");
		comboPais.addItem("+260 Zâmbia");
		comboPais.addItem("+263 Zimbábue");

		// Define o Brasil como selecionado por padrão
		comboPais.setSelectedItem("+55 Brasil");

		comboPais.setBounds(10, 25, 116, 28);
		frame.getContentPane().add(comboPais);

		JLabel lblTelefone = new JLabel("DD");
		lblTelefone.setBounds(10, 6, 70, 14);
		frame.getContentPane().add(lblTelefone);

		txtMensagem = new JTextPane();
		txtMensagem.setBorder(new TitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		txtMensagem.setBounds(10, 103, 490, 108);
		frame.getContentPane().add(txtMensagem);

		MaskFormatter telefoneMask = new MaskFormatter("(##) #####-####");
		telefoneMask.setPlaceholderCharacter('_');

		txtTelefone = new JFormattedTextField(telefoneMask);
		txtTelefone.setBounds(136, 25, 125, 28);
		frame.getContentPane().add(txtTelefone);

		txtCaminho = new JTextField(System.getProperty("user.home") + File.separator + "Downloads");
		txtCaminho.setBounds(10, 432, 366, 28);
		frame.getContentPane().add(txtCaminho);
		txtCaminho.setColumns(10);

		btnSelecionarCaminho = new JButton("...");
		btnSelecionarCaminho.setBounds(388, 432, 50, 28);
		frame.getContentPane().add(btnSelecionarCaminho);

		btnSelecionarCaminho.addActionListener(e -> {

			if (!validarTudo(false)) { // false = só valida o necessário para montar link
				return;
			}

			JFileChooser chooser = new JFileChooser(txtCaminho.getText());
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			int retorno = chooser.showOpenDialog(frame);
			if (retorno == JFileChooser.APPROVE_OPTION) {
				File arquivoSelecionado = chooser.getSelectedFile();
				txtCaminho.setText(arquivoSelecionado.getAbsolutePath());
			}
		});

		JButton btnLink = new JButton("Montar link");
		btnLink.addActionListener(e -> {

			try {
				if (validarTudo(false)) {
					String ddi = comboPais.getSelectedItem().toString().split(" ")[0].replace("+", "");
					String fone = txtTelefone.getText().replaceAll("[^0-9]", "");

					// Prevent double country code
					if (fone.startsWith(ddi)) {
						fone = fone.substring(ddi.length());
					} else if (fone.startsWith("0")) {
						fone = fone.substring(1);
					}

					String msg = URLEncoder.encode(txtMensagem.getText().trim(), StandardCharsets.UTF_8);

					String linkFinal = "https://wa.me/" + ddi + fone + (msg.isEmpty() ? "" : "?text=" + msg);
					txfLink.setText(linkFinal);
				}
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(frame, "Erro ao montar link:\n" + ex.getMessage());
			}
		});
		btnLink.setBounds(321, 235, 179, 28);
		frame.getContentPane().add(btnLink);

		lblNewLabel_3 = new JLabel("Local de salvamento");
		lblNewLabel_3.setBounds(10, 414, 116, 16);
		frame.getContentPane().add(lblNewLabel_3);

		JButton btnNewButton = new JButton(">");
		btnNewButton.addActionListener(e -> {
			File pasta = new File(txtCaminho.getText());

			if (pasta.exists() && pasta.isDirectory()) {
				try {
					java.awt.Desktop.getDesktop().open(pasta);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} else {
				JOptionPane.showMessageDialog(frame, "Caminho inválido ou não existe!");
			}
		});
		btnNewButton.setBounds(449, 432, 51, 28);
		frame.getContentPane().add(btnNewButton);

		JTextPane txtMenssagem = new JTextPane();
		txtMenssagem.setBounds(10, 103, 490, 108);
		frame.getContentPane().add(txtMenssagem);
		
		JLabel lblNewLabel_4 = new JLabel("Mensagem");
		lblNewLabel_4.setBounds(10, 76, 74, 14);
		frame.getContentPane().add(lblNewLabel_4);
		
		JLabel lblTelefone_2 = new JLabel("Telefone");
		lblTelefone_2.setBounds(137, 6, 70, 14);
		frame.getContentPane().add(lblTelefone_2);

	}

	public void setVisible(boolean b) {

		frame.setVisible(b);
	}

	private void gerarComZXing(String path, String texto, String nomeQrCodeGerado, int tamanho) {

		try {

			File myFile = new File(path + "/" + nomeQrCodeGerado + "." + formatoQrCodeGerado);
			Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
			hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
			QRCodeWriter qrCodeWriter = new QRCodeWriter();
			BitMatrix byteMatrix = qrCodeWriter.encode(texto, BarcodeFormat.QR_CODE, tamanho, tamanho, hintMap);
			int CrunchifyWidth = byteMatrix.getWidth();
			BufferedImage image = new BufferedImage(CrunchifyWidth, CrunchifyWidth, BufferedImage.TYPE_INT_RGB);
			image.createGraphics();

			Graphics2D graphics = (Graphics2D) image.getGraphics();
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, CrunchifyWidth, CrunchifyWidth);
			graphics.setColor(Color.BLACK);

			for (int i = 0; i < CrunchifyWidth; i++) {
				for (int j = 0; j < CrunchifyWidth; j++) {
					if (byteMatrix.get(i, j)) {
						graphics.fillRect(i, j, 1, 1);
					}
				}
			}
			ImageIO.write(image, formatoQrCodeGerado, myFile);
			JOptionPane.showMessageDialog(frame, "QRCode gerado em:" + myFile.getAbsolutePath());
		} catch (WriterException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
