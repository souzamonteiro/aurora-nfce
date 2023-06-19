/*
 * Copyright(C) 2021-2022, Roberto Luiz Souza Monteiro. Todos os direitos reservados.
 *
 * Todos os textos, imagens, gráficos, animações, vídeos, músicas, sons e outros materiais
 * são protegidos por direitos autorais e outros direitos de propriedade intelectual
 * pertencentes à Roberto Luiz Souza Monteiro, suas subsidiárias, afiliadas e licenciantes.
 *
 * Roberto Luiz Souza Monteiro é, também, proprietário dos direitos autorais de desenvolvimento,
 * seleção, coordenação, diagramação e disposição dos materiais neste site ou aplicativo.
 * É expressamente vedada a cópia ou reprodução destes materiais para uso ou distribuição comercial,
 * a modificação destes materiais, sua inclusão em outros websites e o seu envio e publicação em
 * outros meios digitais e físicos, ou de qualquer outra forma dispor de tais materiais sem a
 * devida autorização, estando sujeito às responsabilidades e sanções legais.
 *
 * @author Roberto Luiz Souza Monteiro (roberto@souzamonteiro.com)
 *
 */

package com.souzamonteiro.nfcemonitor;

import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.*;
import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.dom.Evento;
import br.com.swconsultoria.nfe.exception.NfeException;
import br.com.swconsultoria.nfe.schema.envcce.TEnvEvento;
import br.com.swconsultoria.nfe.schema.envcce.TRetEnvEvento;
import br.com.swconsultoria.nfe.schema_4.enviNFe.*;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.*;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.COFINS;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.COFINS.COFINSAliq;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.ICMS;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.PIS;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.PIS.PISAliq;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Prod;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Total.ICMSTot;
import br.com.swconsultoria.nfe.schema_4.inutNFe.TInutNFe;
import br.com.swconsultoria.nfe.schema_4.inutNFe.TRetInutNFe;
import br.com.swconsultoria.nfe.schema_4.retConsReciNFe.TRetConsReciNFe;
import br.com.swconsultoria.nfe.schema_4.retConsSitNFe.TRetConsSitNFe;
import br.com.swconsultoria.nfe.schema_4.retConsStatServ.TRetConsStatServ;
import br.com.swconsultoria.nfe.util.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.text.MaskFormatter;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import javax.print.PrintService;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.commons.lang3.StringUtils;

import com.github.anastaciocintra.escpos.barcode.QRCode;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.output.PrinterOutputStream;

/**
 * Emissor de NFC-e.
 * 
 * @author  Roberto Luiz Souza Monteiro
 * @version 1.0
 */
public class NFCeMonitor {
    public static void main(String[] args) throws Exception {
        String caminhoNFCeMonitor = System.getProperty("user.dir");
        
        String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
        String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);
        
        JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
        
        int porta = Integer.parseInt(configuracoes.get("porta").toString());
            
        HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);
        server.createContext("/NFeStatusServico", new NFeStatusServicoHandler());
        server.createContext("/NFeAutorizacao", new NFeAutorizacaoHandler());
        server.createContext("/NFeConsulta", new NFeConsultaHandler());
        server.createContext("/NFeCartaCorrecao", new NFeCartaCorrecaoHandler());
        server.createContext("/NFeCancelamento", new NFeCancelamentoHandler());
        server.createContext("/NFeInutilizacao", new NFeInutilizacaoHandler());
        server.createContext("/NFeImpressao", new NFeImpressaoHandler());
        server.setExecutor(null);
        server.start();
    }
    
    static public String readFile(String filePath) {
        String data = new String();

        try {
            File file = new File(filePath);
            Scanner scanner = new Scanner(file);
            String line;
            
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                data = data + line;
            }
        } catch (FileNotFoundException e) {
            System.out.println("Erro: " + e);
        }
        
        return data;
    }
    
    /**
     * Mapeia os parâmetros passados na URL.
     * 
     * @param   query  String contendo os parâmetros passados na URL.
     * @return         Map contendo os parâmetros passados na URL.
     */
    static public Map<String, String> queryToMap(String query) {
        if(query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }else{
                result.put(entry[0], "");
            }
        }
        return result;
    }
    
    /**
     * Preenche os dados do QR Code.
     * 
     * @param   enviNFe  Dados da NF-e.
     * @param   config   Configurações do certificado digital e do serviço NF-e.
     * @param   idToken  Identificador do CSC.
     * @param   csc      CSC de autenticação da NF-e.
     * @return           Map contendo os parâmetros passados na URL.
     */
    private static String preencheQRCode(TEnviNFe enviNFe, ConfiguracoesNfe config, String idToken, String csc) throws NfeException, NoSuchAlgorithmException {
        return NFCeUtil.getCodeQRCode(
            enviNFe.getNFe().get(0).getInfNFe().getId().substring(3),
            config.getAmbiente().getCodigo(),
            idToken,
            csc,
            WebServiceUtil.getUrl(config,DocumentoEnum.NFCE, ServicosEnum.URL_QRCODE));
    }
    
    /**
     * Preenche os dados do QR Code de contingência.
     * 
     * @param   enviNFe  Dados da NF-e.
     * @param   config   Configurações do certificado digital e do serviço NF-e.
     * @param   idToken  Identificador do CSC.
     * @param   csc      CSC de autenticação da NF-e.
     * @return           Map contendo os parâmetros passados na URL.
     */
    private static String preencheQRCodeContingencia(TEnviNFe enviNFe, ConfiguracoesNfe config, String idToken, String csc) throws NfeException, NoSuchAlgorithmException {
        return NFCeUtil.getCodeQRCodeContingencia(
            enviNFe.getNFe().get(0).getInfNFe().getId().substring(3),
            config.getAmbiente().getCodigo(),
            enviNFe.getNFe().get(0).getInfNFe().getIde().getDhEmi(),
            enviNFe.getNFe().get(0).getInfNFe().getTotal().getICMSTot().getVNF(),
            Base64.getEncoder().encodeToString(enviNFe.getNFe().get(0).getSignature().getSignedInfo().getReference().getDigestValue()),
            idToken,
            csc,
            WebServiceUtil.getUrl(config, DocumentoEnum.NFCE, ServicosEnum.URL_QRCODE));
    }
    
    /**
     * Formata uma string.
     * 
     * @param   texto    String a ser formatada.
     * @param   mascara  Máscara de formatação.
     * @return           String formatada.
     */
    public static String formatarString(String texto, String mascara) {
        try {
            MaskFormatter mf = new MaskFormatter(mascara);
            mf.setValueContainsLiteralCharacters(false);
            
            return mf.valueToString(texto);
        } catch (Exception e) {
            System.out.println(e.toString());
            
            return texto;
        }
        
    }
    
    /**
     * Formata uma chave de acesso de uma NF-e para impressão no DANFE.
     * 
     * @param   chave  Chave a ser formatada.
     * @return         Chave formatada.
     */
    public static String formataChaveNFe(String chave) {
        String chaveFormatada = "";
        
        // Chave: 13230533630582000149650010000001391000001408
        // Pos:   01234567890123456789012345678901234567890123
        // 0123 4567 8901 2345 6789 0123 4567 8901 2345 6789 0123
        chaveFormatada += chave.substring(0, 4) + " ";
        chaveFormatada += chave.substring(4, 8) + " ";
        chaveFormatada += chave.substring(8, 12) + " ";
        chaveFormatada += chave.substring(12, 16) + " ";
        chaveFormatada += chave.substring(16, 20) + " ";
        chaveFormatada += chave.substring(20, 24) + " ";
        chaveFormatada += chave.substring(24, 28) + " ";
        chaveFormatada += chave.substring(28, 32) + " ";
        chaveFormatada += chave.substring(32, 36) + " ";
        chaveFormatada += chave.substring(36, 40) + " ";
        chaveFormatada += chave.substring(40, 44);
                
        return chaveFormatada;
    }
    
    /**
     * Formata uma data de recibo de uma NF-e para impressão no DANFE.
     * 
     * @param   data  Data a ser formatada.
     * @return        Data formatada.
     */
    public static String formataDataNFe(String data) {
        String dataFormatada = "";
        
        // Data: 2023-05-23T10:42:23-04:00
        // Pos:  0123456789012345678901234
        // 01234569 12345678
        dataFormatada += data.substring(0, 10) + " ";
        dataFormatada += data.substring(11, 19);
                
        return dataFormatada;
    }
    
    /**
     * Imprime o DANFE em uma impressora ESC/POS.
     * 
     * @param   json            Dados da NF-e.
     * @param   nomeImpressora  Nome da impressora ESC/POS.
     * @param   numeroColunas   Tamanho do papel (58mm ou 80mm).
     * @return                  Map contendo os parâmetros passados na URL.
     */
    private static int imprimeDANFE(JSONObject json, String nomeImpressora, String numeroColunas) {
        System.out.println(json.toString());
        
        /*
         * Meio de pagamento da venda:
         * 01 = Dinheiro;
         * 02 = Cheque;
         * 03 = Cartão de crédito;
         * 04 = Cartão de débito;
         * 05 = Crédito loja;
         * 10 = Vale alimentação;
         * 11 = Vale refeição;
         * 12 = Vale presente;
         * 13 = Vale combustível;
         * 14 = Duplicata mercantil;
         * 15 = Boleto bancário;
         * 90 = Sem pagamento;
         * 99 = Outros.
         */
        Map<String, String> meioPagamento;
        meioPagamento = new LinkedHashMap<>();
        
        meioPagamento.put("01", "Dinheiro");
        meioPagamento.put("02", "Cheque");
        meioPagamento.put("03", "Cartão de crédito");
        meioPagamento.put("04", "Cartão de débito");
        meioPagamento.put("05", "Crédito loja");
        meioPagamento.put("10", "Vale alimentação");
        meioPagamento.put("11", "Vale refeição");
        meioPagamento.put("12", "Vale presente");
        meioPagamento.put("13", "Vale combustível");
        meioPagamento.put("14", "Duplicata mercantil");
        meioPagamento.put("15", "Boleto bancário");
        meioPagamento.put("90", "Sem pagamento");
        meioPagamento.put("99", "Outros");
        
        // Formatador de números.
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        
        int caracteresLinha = Integer.parseInt(numeroColunas);
        
        try {
            // Inicializa o acesso ao serviço de impressão.
            PrintService printService = PrinterOutputStream.getPrintServiceByName(nomeImpressora);
            PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);

            // Cria o objeto de impressão ESC/POS.
            EscPos escpos = new EscPos(printerOutputStream);

            // O papel de 58mm permite imprimir 30, 32 ou 40 caracteres (colunas) por linha.
            // O papel de 80mm permite imprimir 42 ou 56 caracteres (colunas) por linha.
            
            // Layout 58mm:
            //
            // 0         1         2         3
            // 01234567890123456789012345678901
            // --------------------------------
            //       CNPJ: 33630582000149
            // Editora Roberto Luiz Souza Monte
            //               iro
            // Rua Chile, s/n, Edifício Eduardo
            //       De Moraes, sala 606
            //      Centro, Salvador, BA
            //      Documento Auxiliar da
            //         Nota Fiscal de
            //      Consumidor Eletrônica
            // --------------------------------
            // Código  |Descrição
            // Qtde    |UN |  Vl Unit| Vl Total
            // 12345678 123456789012345678901234
            // 00000001|Banqueta plástica dobrá
            // vel, branca, altura 220 mm
            // 12345678 123 123456789 123456789
            //     1,00|PC |    56,08|    56,08
            // 00000002|Jogo de cinta com catra
            // ca para amarração de carga 0,8 
            // tf/0,4 tf, CC 080
            //     1,00|PC |    37,82|    75,64
            // 00000003|Óleo de cambio manual S
            // AE 80W Flex Oil 1Lt
            //     1,00|UN |    13,00|    13,00
            // --------------------------------
            // Qtde. total de itens           3
            // Valor total R$            144,72
            // Desconto R$                 1,30
            // Valor a Pagar R$          143,42
            // FORMA PAGAMENTO    VALOR PAGO R$
            // Dinheiro                   50,00
            // Cartão de Crédito          40,00
            // Cartão de Débito          100,00
            // Troco R$                   46,58
            // --------------------------------
            //  Consulte pela Chave de Acesso e
            //  m https://sistemas.sefaz.am.gov
            //  .br/nfceweb-hom/formConsulta.do
            //  1323 0533 6305 8200 0149 6500
            //    1000 0003 1810 0000 3193
            // --------------------------------
            // CONSUMIDOR - CPF: 40325635862 -
            //     Andréia Rita da Silva
            // --------------------------------
            //     NFC-e n.: 318 Série: 1
            //      2023-05-19 08:24:28
            //
            //   Protocolo de autorização:
            //       113230010671996
            //      Data de autorização:
            //      2023-05-19 07:24:33
            //
            //     EMITIDA EM AMBIENTE DE
            //  HOMOLOGAÇÃO - SEM VALOR FISCAL
            //
            //        +--------------+
            //        |              |
            //        |              |
            //        |              |
            //        |              |
            //        |              |
            //        |              |
            //        +--------------+
            //
            // --------------------------------
            // Tributos Totais Incidentes (Lei
            //  Federal 12.741/2012): R$73,27.
            //  Trib aprox R$: 46,68 Fed, 26,59
            //  Est e 0,00 Mun. Fonte: IBPT.
            
            // Layout 80mm:
            //
            // 0         1         2         3         4
            // 012345678901234567890123456789012345678901234567
            //              Documento Auxiliar da
            //                  Nota Fiscal de
            //              Consumidor Eletrônica
            // ------------------------------------------------
            // Código  |Descrição
            // Qtde    |UN |         Vl Unit|          Vl Total
            // 12345678 123456789012345678901234567890123456789
            // 00000001|Banqueta plástica dobrável, branca, alt
            // ura 220 mm
            // 123456789012
            // 12345678 123 12345678901234567 12345678901234567
            //     1,00|PC |            56,08|            56,08
            // ------------------------------------------------
            // 0         1         2         3
            // 0         1         2         3         4
            // 012345678901234567890123456789012345678901234567
            // Qtde. total de itens                           3 tam = 20
            // Valor total R$                            144,72 tam = 14
            // Desconto R$                                 1,30 tam = 11
            // Valor a Pagar R$                          143,42 tam = 16
            // 123456789012345
            // FORMA PAGAMENTO                    VALOR PAGO R$
            // Dinheiro                                   50,00 tam = tamanho("Dinheiro")
            // Cartão de Crédito                          40,00 tam = tamanho("Cartão de Crédito")
            // Cartão de Débito                          100,00 tam = tamanho("Cartão de Débito")
            // Troco R$                                   46,58 tam = 8
            // ------------------------------------------------
            
            // Inicializa a impressora.
            escpos.initializePrinter();
            // Seleciona a página de código Latin-1.
            escpos.setPrinterCharacterTable(3);

            //escpos.writeLF("0         1         2         3         4       ");
            //escpos.writeLF("012345678901234567890123456789012345678901234567");
            
            // Lê o objeto contendo os dados da NFC-e.
            JSONObject jsonInfNFe = json.getJSONObject("infNFe");
            JSONObject jsonIde = jsonInfNFe.getJSONObject("ide");
            JSONObject jsonEmit = jsonInfNFe.getJSONObject("emit");
            JSONObject jsonEnderEmit = jsonEmit.getJSONObject("enderEmit");
            JSONObject jsonDest = jsonInfNFe.getJSONObject("dest");
            JSONArray jsonDet = jsonInfNFe.getJSONArray("det");
            JSONObject jsonTotal = jsonInfNFe.getJSONObject("total");
            JSONObject jsonICMSTot = jsonTotal.getJSONObject("ICMSTot");
            JSONObject jsonPag = jsonInfNFe.getJSONObject("pag");
            JSONArray jsonDetPag = jsonPag.getJSONArray("detPag");
            JSONObject jsonInfAdic = jsonInfNFe.getJSONObject("infAdic");
            JSONObject jsonInfProt = json.getJSONObject("infProt");

            // Dados do emitente.
            String emitCNPJ = jsonEmit.get("CNPJ").toString();
            String emitXNome = jsonEmit.get("xNome").toString();
            String emitXLgr = jsonEnderEmit.get("xLgr").toString();
            String emitNro = jsonEnderEmit.get("nro").toString();
            String emitXCpl = jsonEnderEmit.get("xCpl").toString();
            String emitXBairro = jsonEnderEmit.get("xBairro").toString();
            String emitXMun = jsonEnderEmit.get("xMun").toString();
            String emitUF = jsonEnderEmit.get("UF").toString();

            // Imprime o cabeçalho do DANFE.
            escpos.writeLF(StringUtils.center("CNPJ: " + formatarString(emitCNPJ, "##.###.###/####-##"), caracteresLinha));
            escpos.writeLF(StringUtils.center(emitXNome, caracteresLinha));
            escpos.writeLF(StringUtils.center(emitXLgr + ", " + emitNro, caracteresLinha));
            escpos.writeLF(StringUtils.center(emitXCpl, caracteresLinha));
            escpos.writeLF(StringUtils.center(emitXBairro + ", " + emitXMun + ", " + emitUF, caracteresLinha));
            
            escpos.writeLF(StringUtils.center("Documento Auxiliar da Nota Fiscal de Consumidor Eletrônica", caracteresLinha));
            
            // Dados da autorização.
            String tpAmb = jsonInfProt.get("tpAmb").toString();
            String tpEmis = jsonInfProt.get("tpEmis").toString();
            String chNFe = jsonInfProt.get("chNFe").toString();
            String dhRecbto = jsonInfProt.get("dhRecbto").toString();
            String nProt = jsonInfProt.get("nProt").toString();
            String urlChave = jsonInfProt.get("urlChave").toString();
            String qrCode = jsonInfProt.get("qrCode").toString();

            if (tpEmis.equals("9")) {
                escpos.writeLF(StringUtils.center("EMITIDA EM CONTINGÊNCIA", caracteresLinha));
                escpos.writeLF(StringUtils.center("Pendente de autorização", caracteresLinha));
            }
            
            escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));
            
            escpos.writeLF(StringUtils.rightPad("Código", 8) + "|" + StringUtils.rightPad("Descrição", caracteresLinha - 9));
            escpos.writeLF(StringUtils.rightPad( "Qtde", 8) + "|" + StringUtils.rightPad("UN", 3) + "|" + StringUtils.leftPad("Vl Unit", (caracteresLinha - 14) / 2) + "|" + StringUtils.leftPad("Vl Total", (caracteresLinha - 14) / 2));
            
            // Produtos constantes da nota.
            int numeroProdutos = jsonDet.length();
            for (int i = 0; i < numeroProdutos; i++) {
                JSONObject itemDet = jsonDet.getJSONObject(i);
                JSONObject jsonProd = itemDet.getJSONObject("prod");

                String cProd = jsonProd.get("cProd").toString();
                String xProd;
                if (tpAmb.equals("2")) {
                    xProd = "NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
                } else {
                    xProd = jsonProd.get("xProd").toString();
                }
                String qCom = jsonProd.get("qCom").toString();
                String uCom = jsonProd.get("uCom").toString();
                String vUnCom = jsonProd.get("vUnCom").toString();
                String vProduto = jsonProd.get("vProd").toString();
                
                escpos.writeLF(StringUtils.leftPad(cProd, 8, "0") + "|" + StringUtils.abbreviate(xProd, caracteresLinha - 9));
                escpos.writeLF(StringUtils.leftPad( numberFormat.format(Float.parseFloat(qCom)), 8) + "|" + StringUtils.rightPad(uCom, 3) + "|" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vUnCom)), (caracteresLinha - 14) / 2) + "|" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vProduto)), (caracteresLinha - 14) / 2));
            }

            // Totais da nota.
            String vProd = jsonICMSTot.get("vProd").toString();
            String vDesc = jsonICMSTot.get("vDesc").toString();
            String vNF = jsonICMSTot.get("vNF").toString();
            
            escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));
            
            escpos.writeLF("Qtde. total de itens" + StringUtils.leftPad(String.valueOf(numeroProdutos), caracteresLinha - 20));
            escpos.writeLF("Valor total R$" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vProd)), caracteresLinha - 14));
            escpos.writeLF("Desconto R$" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vDesc)), caracteresLinha - 11));
            escpos.writeLF("Valor a Pagar R$" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vNF)), caracteresLinha - 16));
            
            escpos.writeLF("FORMA PAGAMENTO" + StringUtils.leftPad("VALOR PAGO R$", caracteresLinha - 15));
            
            // Meios de pagamento.
            for (int i = 0; i < jsonDetPag.length(); i++) {
                JSONObject jsonItemDetPag = jsonDetPag.getJSONObject(i);
                String tPag = meioPagamento.get(jsonItemDetPag.get("tPag").toString());
                String vPag = jsonItemDetPag.get("vPag").toString();

                escpos.writeLF(tPag + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vPag)), caracteresLinha - tPag.length()));
            }

            // Valor do troco.
            String vTroco;
            if (jsonPag.has("vTroco")) {
                vTroco = jsonPag.get("vTroco").toString();
            } else {
                vTroco = "0.00";
            }
            escpos.writeLF("Troco R$" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vTroco)), caracteresLinha - 8));
            
            
            escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));
            
            
            escpos.writeLF(StringUtils.center("Consulte pela Chave de Acesso em " + urlChave, caracteresLinha));
            escpos.writeLF(StringUtils.center(formataChaveNFe(chNFe), caracteresLinha));
            
            
            escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));
            
            
            // Dados do consumidor.
            String destCNPJ;
            if (jsonDest.has("CPF")) {
                destCNPJ = " - CPF: " + formatarString(jsonDest.get("CPF").toString(), "###.###.###-##");
            } else if (jsonDest.has("CNPJ")) {
                destCNPJ = " - CNPJ: " + formatarString(jsonDest.get("CNPJ").toString(), "##.###.###/####-##");
            } else {
                destCNPJ = " NÃO IDENTIFICADO";
            }
            String destXNome;
            if (jsonDest.has("xNome")) {
                destXNome = " - " + jsonDest.get("xNome").toString();
            } else {
                destXNome = "";
            }
            escpos.writeLF(StringUtils.center("CONSUMIDOR" + destCNPJ + destXNome, caracteresLinha));
            
            
            escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));
            
            // Dados da NF-e.
            String serie = jsonIde.get("serie").toString();
            String nNF = jsonIde.get("nNF").toString();
            String dhEmi = jsonIde.get("dhEmi").toString();

            escpos.writeLF(StringUtils.center("NFC-e n.: " + nNF + " Série: " + serie, caracteresLinha));
            escpos.writeLF(StringUtils.center(dhEmi, caracteresLinha));
            
            if (tpEmis.equals("1")) {
                escpos.writeLF(StringUtils.center("Protocolo de autorização:", caracteresLinha));
                escpos.writeLF(StringUtils.center(nProt, caracteresLinha));
                escpos.writeLF(StringUtils.center("Data de autorização:", caracteresLinha));
                escpos.writeLF(StringUtils.center(formataDataNFe(dhRecbto), caracteresLinha));
            } else {
                escpos.writeLF(StringUtils.center("Via consumidor", caracteresLinha));
                escpos.writeLF(StringUtils.center("EMITIDA EM CONTINGÊNCIA", caracteresLinha));
                escpos.writeLF(StringUtils.center("Pendente de autorização", caracteresLinha));
            }
            
            if (tpAmb.equals("2")) {
                escpos.writeLF(StringUtils.center("EMITIDA EM AMBIENTE DE", caracteresLinha));
                escpos.writeLF(StringUtils.center("HOMOLOGAÇÃO - SEM VALOR FISCAL", caracteresLinha));
            }
            
            escpos.writeLF("");
            
            // Imprime o QR Code.
            QRCode qrcode = new QRCode();
            qrcode.setSize(6);
            qrcode.setJustification(EscPosConst.Justification.Center);
            escpos.write(qrcode, qrCode);

            // Informações complementares.
            
            escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));
            
            String infCpl = jsonInfAdic.get("infCpl").toString();
            escpos.writeLF(infCpl);
            
            // Corta o papel se a impressora suportar.
            escpos.feed(5).cut(EscPos.CutMode.FULL);
            
            if (tpEmis.equals("9")) {
                // Imprime o cabeçalho do DANFE.
                escpos.writeLF(StringUtils.center("CNPJ: " + formatarString(emitCNPJ, "##.###.###/####-##"), caracteresLinha));
                escpos.writeLF(StringUtils.center(emitXNome, caracteresLinha));
                escpos.writeLF(StringUtils.center(emitXLgr + ", " + emitNro, caracteresLinha));
                escpos.writeLF(StringUtils.center(emitXCpl, caracteresLinha));
                escpos.writeLF(StringUtils.center(emitXBairro + ", " + emitXMun + ", " + emitUF, caracteresLinha));

                escpos.writeLF(StringUtils.center("Documento Auxiliar da Nota Fiscal de Consumidor Eletrônica", caracteresLinha));
                
                escpos.writeLF(StringUtils.center("EMITIDA EM CONTINGÊNCIA", caracteresLinha));
                escpos.writeLF(StringUtils.center("Pendente de autorização", caracteresLinha));

                escpos.writeLF(StringUtils.rightPad("Código", 8) + "|" + StringUtils.rightPad("Descrição", caracteresLinha - 9));
                escpos.writeLF(StringUtils.rightPad( "Qtde", 8) + "|" + StringUtils.rightPad("UN", 3) + "|" + StringUtils.leftPad("Vl Unit", (caracteresLinha - 14) / 2) + "|" + StringUtils.leftPad("Vl Total", (caracteresLinha - 14) / 2));
                
                // Produtos constantes da nota.
                for (int i = 0; i < numeroProdutos; i++) {
                    JSONObject itemDet = jsonDet.getJSONObject(i);
                    JSONObject jsonProd = itemDet.getJSONObject("prod");

                    String cProd = jsonProd.get("cProd").toString();
                    String xProd;
                    if (tpAmb.equals("2")) {
                        xProd = "NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL";
                    } else {
                        xProd = jsonProd.get("xProd").toString();
                    }
                    String qCom = jsonProd.get("qCom").toString();
                    String uCom = jsonProd.get("uCom").toString();
                    String vUnCom = jsonProd.get("vUnCom").toString();
                    String vProduto = jsonProd.get("vProd").toString();
                    
                    escpos.writeLF(StringUtils.leftPad(cProd, 8, "0") + "|" + StringUtils.abbreviate(xProd, caracteresLinha - 9));
                    escpos.writeLF(StringUtils.leftPad( numberFormat.format(Float.parseFloat(qCom)), 8) + "|" + StringUtils.rightPad(uCom, 3) + "|" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vUnCom)), (caracteresLinha - 14) / 2) + "|" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vProduto)), (caracteresLinha - 14) / 2));
                }

                // Totais da nota.
                
                escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));
                
                escpos.writeLF("Qtde. total de itens" + StringUtils.leftPad(String.valueOf(numeroProdutos), caracteresLinha - 20));
                escpos.writeLF("Valor total R$" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vProd)), caracteresLinha - 14));
                escpos.writeLF("Desconto R$" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vDesc)), caracteresLinha - 11));
                escpos.writeLF("Valor a Pagar R$" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vNF)), caracteresLinha - 16));

                escpos.writeLF("FORMA PAGAMENTO" + StringUtils.leftPad("VALOR PAGO R$", caracteresLinha - 15));
                
                // Meios de pagamento.
                for (int i = 0; i < jsonDetPag.length(); i++) {
                    JSONObject jsonItemDetPag = jsonDetPag.getJSONObject(i);
                    String tPag = meioPagamento.get(jsonItemDetPag.get("tPag").toString());
                    String vPag = jsonItemDetPag.get("vPag").toString();

                    escpos.writeLF(tPag + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vPag)), caracteresLinha - tPag.length()));
                }

                // Valor do troco.
                escpos.writeLF("Troco R$" + StringUtils.leftPad(numberFormat.format(Float.parseFloat(vTroco)), caracteresLinha - 8));

                
                escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));


                escpos.writeLF(StringUtils.center("Consulte pela Chave de Acesso em " + urlChave, caracteresLinha));
                escpos.writeLF(StringUtils.center(formataChaveNFe(chNFe), caracteresLinha));

                
                escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));


                // Dados do consumidor.
                escpos.writeLF(StringUtils.center("CONSUMIDOR" + destCNPJ + destXNome, caracteresLinha));

                
                escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));


                // Dados da NF-e.
                escpos.writeLF(StringUtils.center("NFC-e n.: " + nNF + " Série: " + serie, caracteresLinha));
                escpos.writeLF(StringUtils.center(dhEmi, caracteresLinha));

                escpos.writeLF(StringUtils.center("Via estabelecimento", caracteresLinha));
                escpos.writeLF(StringUtils.center("EMITIDA EM CONTINGÊNCIA", caracteresLinha));
                escpos.writeLF(StringUtils.center("Pendente de autorização", caracteresLinha));

                if (tpAmb.equals("2")) {
                    escpos.writeLF(StringUtils.center("EMITIDA EM AMBIENTE DE", caracteresLinha));
                    escpos.writeLF(StringUtils.center("HOMOLOGAÇÃO - SEM VALOR FISCAL", caracteresLinha));
                }
                
                escpos.writeLF("");
                
                // Imprime o QR Code.
                escpos.write(qrcode, qrCode);

                // Informações complementares.
                
                escpos.writeLF(StringUtils.rightPad("-", caracteresLinha, "-"));

                escpos.writeLF(infCpl);

                // Corta o papel se a impressora suportar.
                escpos.feed(5).cut(EscPos.CutMode.FULL);
            }
            
            escpos.close();
        } catch (Exception e) {
            System.out.println(e.toString());
            
            return 0;
        }
        
        return 1;
    }
    
    /**
     * Processa o serviço NFeImpressao.
     */
    static class NFeImpressaoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String caminhoNFCeMonitor = System.getProperty("user.dir");
        
            String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
            String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);

            JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
            
            String habilitarServicoImpressao = "0";
            String nomeImpressora = "";
            String numeroColunas = "58";
            if (configuracoes.has("habilitarServicoImpressao")) {
                habilitarServicoImpressao = configuracoes.get("habilitarServicoImpressao").toString();
            }
            if (configuracoes.has("nomeImpressora")) {
                nomeImpressora = configuracoes.get("nomeImpressora").toString();
            }
            if (configuracoes.has("numeroColunas")) {
                numeroColunas = configuracoes.get("numeroColunas").toString();
            }
            
            InputStream inputStream = httpExchange.getRequestBody(); 
            Scanner scanner = new Scanner(inputStream);
            String linha;
            String dados = new String();
            
            while (scanner.hasNextLine()) {
                linha = scanner.nextLine();
                dados = dados + linha;
            }
            
            JSONObject json = new JSONObject(dados);
            
            String cStat = "";
            String xMotivo = "";
            
            /*
             * Imprime o DANFE.
             */
            
            if (habilitarServicoImpressao.equals("1")) {
                try {
                    cStat = "100";
                    xMotivo = "Sucesso";
                    
                    if(imprimeDANFE(json, nomeImpressora, numeroColunas) != 1) {
                        cStat = "000";
                        xMotivo = "Ocorreu um erro ao tentar imprimir o DANFE";
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());

                    cStat = "000";
                    xMotivo = e.getMessage();
                }
            } else {
                cStat = "000";
                xMotivo = "Serviço de impressão desabilitado";
            }
            
            JSONObject responseJSON = new JSONObject();
            responseJSON.put("cStat", cStat);
            responseJSON.put("xMotivo", xMotivo);

            System.out.println(responseJSON.toString());

            String response = responseJSON.toString();

            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            httpExchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }
    
    /**
     * Processa o serviço NFeStatusServico.
     */
    static class NFeStatusServicoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String caminhoNFCeMonitor = System.getProperty("user.dir");
        
            String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
            String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);

            JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
            
            InputStream inputStream = httpExchange.getRequestBody(); 
            Scanner scanner = new Scanner(inputStream);
            String linha;
            String dados = new String();
            
            while (scanner.hasNextLine()) {
                linha = scanner.nextLine();
                dados = dados + linha;
            }
            
            JSONObject json = new JSONObject(dados);
            
            String cStat = "";
            String xMotivo = "";
            
            /*
             * Prepara o XML da NFC-e.
             */
            
            // Inicia As configurações.
            ConfiguracoesNfe config;
            
            try {
                config = Config.iniciaConfiguracoes();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
                
                return;
            }
            
            try {
                // Consulta o status do serviço.
                TRetConsStatServ retorno = Nfe.statusServico(config, DocumentoEnum.NFCE);
                
                cStat = retorno.getCStat();
                xMotivo = retorno.getXMotivo();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }
    }
    
    /**
     * Processa o serviço NFeAutorizacao.
     */
    static class NFeAutorizacaoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String caminhoNFCeMonitor = System.getProperty("user.dir");
        
            String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
            String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);

            JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
            
            String webserviceAmbiente = configuracoes.get("webserviceAmbiente").toString();
            String caminhoXML = configuracoes.get("caminhoXML").toString();
            String simularContingencia = "0";
            String imprimirDANFE = "0";
            String nomeImpressora = "";
            String numeroColunas = "58";
            if (configuracoes.has("imprimirDANFE")) {
                imprimirDANFE = configuracoes.get("imprimirDANFE").toString();
            }
            if (configuracoes.has("nomeImpressora")) {
                nomeImpressora = configuracoes.get("nomeImpressora").toString();
            }
            if (configuracoes.has("numeroColunas")) {
                numeroColunas = configuracoes.get("numeroColunas").toString();
            }
            if (configuracoes.has("simularContingencia")) {
                simularContingencia = configuracoes.get("simularContingencia").toString();
            }
            
            InputStream inputStream = httpExchange.getRequestBody(); 
            Scanner scanner = new Scanner(inputStream);
            String linha;
            String dados = new String();
            
            while (scanner.hasNextLine()) {
                linha = scanner.nextLine();
                dados = dados + linha;
            }
            
            JSONObject json = new JSONObject(dados);
            
            String cStat = "";
            String xMotivo = "";
            String dhRecbto = "";
            String nProt = "";
            String tpAmb = "";
            String tpEmis = "";
            String dhCont = "";
            String xJust = "";
            String chave = "";
            String urlChave = "";
            String qrCode = "";
            String xml = "";
            
            /*
             * Prepara o XML da NFC-e.
             */
            
            // Inicia As configurações.
            ConfiguracoesNfe config;
            
            try {
                config = Config.iniciaConfiguracoes();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                responseJSON.put("dhRecbto", dhRecbto);
                responseJSON.put("nProt", nProt);
                responseJSON.put("tpAmb", tpAmb);
                responseJSON.put("tpEmis", tpEmis);
                responseJSON.put("dhCont", dhCont);
                responseJSON.put("xJust", xJust);
                responseJSON.put("chave", chave);
                responseJSON.put("urlChave", urlChave);
                responseJSON.put("qrCode", qrCode);
                responseJSON.put("xml", xml);

                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
                
                return;
            }
            
            JSONObject jsonInfNFe = json.getJSONObject("infNFe");
            JSONObject jsonIde = jsonInfNFe.getJSONObject("ide");
            JSONObject jsonEmit = jsonInfNFe.getJSONObject("emit");
            JSONObject jsonEnderEmit = jsonEmit.getJSONObject("enderEmit");
            JSONObject jsonDest = jsonInfNFe.getJSONObject("dest");
            JSONArray jsonDet = jsonInfNFe.getJSONArray("det");
            JSONObject jsonTotal = jsonInfNFe.getJSONObject("total");
            JSONObject jsonICMSTot = jsonTotal.getJSONObject("ICMSTot");
            JSONObject jsonTransp = jsonInfNFe.getJSONObject("transp");
            JSONObject jsonPag = jsonInfNFe.getJSONObject("pag");
            JSONArray jsonDetPag = jsonPag.getJSONArray("detPag");
            
            // Numero da NFC-e.
            String nNF = jsonIde.get("nNF").toString();
            String cNF = jsonIde.get("cNF").toString();

            int numeroNFCe = Integer.parseInt(nNF);
            int numeroCNF = Integer.parseInt(cNF);
            // Formata o CNF NFC-e com 8 digitos.
            String cnf = ChaveUtil.completarComZerosAEsquerda(String.valueOf(numeroCNF), 8);

            // CNPJ do emitente da NFC-e.
            String cnpj = jsonEmit.get("CNPJ").toString();
            // IE do emitente da NFC-e
            String ie = jsonEmit.get("IE").toString();

            // Data de emissão da NFC-e.
            LocalDateTime dataEmissao = LocalDateTime.now();
            // Modelo da NFC-e.
            String modelo = DocumentoEnum.NFCE.getModelo();
            // Série da NFC-e.
            int serie = Integer.parseInt(jsonIde.get("serie").toString());
            
            // Tipo de ambiente do serviço NF-e.
            tpAmb = config.getAmbiente().getCodigo();
            // Tipo de emissao da NFC-e.
            tpEmis = jsonIde.get("tpEmis").toString();
            
            // Id do token de emissão da NFC-e..
            String idToken = configuracoes.get("idToken").toString();
            // CSC da NFC-e.
            String csc = configuracoes.get("CSC").toString();

            // Chave da NFC-e.
            ChaveUtil chaveUtil = new ChaveUtil(config.getEstado(), cnpj, modelo, serie, numeroNFCe, tpEmis, cnf, dataEmissao);
            chave = chaveUtil.getChaveNF();
            String cdv = chaveUtil.getDigitoVerificador();

            InfNFe infNFe = new InfNFe();
            infNFe.setId(chave);
            infNFe.setVersao(ConstantesUtil.VERSAO.NFE);

            // Preenche a IDE.
            Ide ide = new Ide();
            ide.setCUF(config.getEstado().getCodigoUF());
            ide.setCNF(cnf);
            ide.setNatOp(jsonIde.get("natOp").toString());
            ide.setMod(modelo);
            ide.setSerie(String.valueOf(serie));

            ide.setNNF(String.valueOf(numeroNFCe));
            ide.setDhEmi(XmlNfeUtil.dataNfe(dataEmissao));
            ide.setTpNF(jsonIde.get("tpNF").toString());
            ide.setIdDest(jsonIde.get("idDest").toString());
            ide.setCMunFG(jsonIde.get("cMunFG").toString());
            ide.setTpImp(jsonIde.get("tpImp").toString());
            ide.setTpEmis(tpEmis);
            ide.setCDV(cdv);
            ide.setTpAmb(tpAmb);
            ide.setFinNFe(jsonIde.get("finNFe").toString());
            ide.setIndFinal(jsonIde.get("indFinal").toString());
            ide.setIndPres(jsonIde.get("indPres").toString());
            ide.setProcEmi(jsonIde.get("procEmi").toString());
            ide.setVerProc(jsonIde.get("verProc").toString());
            if (jsonIde.has("dhCont")) {
                ide.setDhCont(jsonIde.get("dhCont").toString());
                ide.setXJust(jsonIde.get("xJust").toString());
            }
            infNFe.setIde(ide);

            // Preenche o Emitente.
            Emit emit = new Emit();
            emit.setCNPJ(cnpj);
            emit.setIE(ie);
            emit.setXNome(jsonEmit.get("xNome").toString());

            TEnderEmi enderEmit = new TEnderEmi();
            enderEmit.setXLgr(jsonEnderEmit.get("xLgr").toString());
            enderEmit.setNro(jsonEnderEmit.get("nro").toString());
            if (jsonEnderEmit.has("xCpl")) {
                enderEmit.setXCpl(jsonEnderEmit.get("xCpl").toString());
            }
            enderEmit.setXBairro(jsonEnderEmit.get("xBairro").toString());
            enderEmit.setCMun(jsonEnderEmit.get("cMun").toString());
            enderEmit.setXMun(jsonEnderEmit.get("xMun").toString());
            enderEmit.setUF(TUfEmi.valueOf(config.getEstado().toString()));
            enderEmit.setCEP(jsonEnderEmit.get("CEP").toString());
            if (jsonEnderEmit.has("cPais")) {
                enderEmit.setCPais(jsonEnderEmit.get("cPais").toString());
            }
            if (jsonEnderEmit.has("xPais")) {
                enderEmit.setXPais(jsonEnderEmit.get("xPais").toString());
            }
            if (jsonEnderEmit.has("fone")) {
                enderEmit.setFone(jsonEnderEmit.get("fone").toString());
            }
            emit.setEnderEmit(enderEmit);

            emit.setCRT(jsonEmit.get("CRT").toString());

            infNFe.setEmit(emit);

            // Preenche o Destinatario.
            Dest dest = new Dest();
            if (jsonDest.has("CNPJ") || jsonDest.has("CPF")) {
                if (jsonDest.has("CNPJ")) {
                    dest.setCNPJ(jsonDest.get("CNPJ").toString());
                }
                if (jsonDest.has("CPF")) {
                    dest.setCPF(jsonDest.get("CPF").toString());
                }
                if (webserviceAmbiente.equals("2")) {
                    dest.setXNome("NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL");
                } else {
                    dest.setXNome(jsonDest.get("xNome").toString());
                }
                if (jsonDest.has("enderDest")) {
                    JSONObject jsonEnderDest = jsonDest.getJSONObject("enderDest");

                    TEndereco enderDest = new TEndereco();
                    enderDest.setXLgr(jsonEnderDest.get("xLgr").toString());
                    enderDest.setNro(jsonEnderDest.get("nro").toString());
                    if (jsonEnderDest.has("xCpl")) {
                        enderDest.setXCpl(jsonEnderDest.get("xCpl").toString());
                    }
                    enderDest.setXBairro(jsonEnderDest.get("xBairro").toString());
                    enderDest.setCMun(jsonEnderDest.get("cMun").toString());
                    enderDest.setXMun(jsonEnderDest.get("xMun").toString());
                    enderDest.setUF(TUf.valueOf(jsonEnderDest.get("UF").toString()));
                    if (jsonEnderDest.has("CEP")) {
                        enderDest.setCEP(jsonEnderDest.get("CEP").toString());
                    }
                    if (jsonEnderDest.has("cPais")) {
                        enderDest.setCPais(jsonEnderDest.get("cPais").toString());
                    }
                    if (jsonEnderDest.has("xPais")) {
                        enderDest.setXPais(jsonEnderDest.get("xPais").toString());
                    }
                    if (jsonEnderDest.has("fone")) {
                        enderDest.setFone(jsonEnderDest.get("fone").toString());
                    }
                    dest.setEnderDest(enderDest);
                }
                dest.setIndIEDest("9");
                
                infNFe.setDest(dest);
            }
            
            // Preenche os dados do contador.
            if (jsonInfNFe.has("autXML")) {
                JSONObject jsonAutXML = jsonInfNFe.getJSONObject("autXML");

                AutXML autXML = new AutXML();
                if (jsonAutXML.has("CPF")) {
                    autXML.setCPF(jsonAutXML.get("CPF").toString());
                }
                if (jsonAutXML.has("CNPJ")) {
                    autXML.setCNPJ(jsonAutXML.get("CNPJ").toString());
                }
                infNFe.getAutXML().add(autXML);
            }
            
            // Preenche os dados do Produto da NFC-e e adiciona à lista de produtos.
            for (int i = 0; i < jsonDet.length(); i++) {
                JSONObject itemDet = jsonDet.getJSONObject(i);

                JSONObject jsonProd = itemDet.getJSONObject("prod");
                JSONObject jsonImposto = itemDet.getJSONObject("imposto");
                JSONObject jsonICMS = jsonImposto.getJSONObject("ICMS");
                JSONObject jsonICMS00 = new JSONObject();
                JSONObject jsonICMS10 = new JSONObject();
                JSONObject jsonICMS20 = new JSONObject();
                JSONObject jsonICMS30 = new JSONObject();
                JSONObject jsonICMS40 = new JSONObject();
                JSONObject jsonICMS51 = new JSONObject();
                JSONObject jsonICMS60 = new JSONObject();
                JSONObject jsonICMS70 = new JSONObject();
                JSONObject jsonICMS90 = new JSONObject();
                JSONObject jsonICMSSN101 = new JSONObject();
                JSONObject jsonICMSSN102 = new JSONObject();
                JSONObject jsonICMSSN201 = new JSONObject();
                JSONObject jsonICMSSN202 = new JSONObject();
                JSONObject jsonICMSSN500 = new JSONObject();
                JSONObject jsonICMSSN900 = new JSONObject();
                if (jsonICMS.has("ICMS00")) {
                    jsonICMS00 = jsonICMS.getJSONObject("ICMS00");
                }
                if (jsonICMS.has("ICMS10")) {
                    jsonICMS10 = jsonICMS.getJSONObject("ICMS10");
                }
                if (jsonICMS.has("ICMS20")) {
                    jsonICMS20 = jsonICMS.getJSONObject("ICMS20");
                }
                if (jsonICMS.has("ICMS30")) {
                    jsonICMS30 = jsonICMS.getJSONObject("ICMS30");
                }
                if (jsonICMS.has("ICMS40")) {
                    jsonICMS40 = jsonICMS.getJSONObject("ICMS40");
                }
                if (jsonICMS.has("ICMS51")) {
                    jsonICMS51= jsonICMS.getJSONObject("ICMS51");
                }
                if (jsonICMS.has("ICMS60")) {
                    jsonICMS60 = jsonICMS.getJSONObject("ICMS60");
                }
                if (jsonICMS.has("ICMS70")) {
                    jsonICMS70 = jsonICMS.getJSONObject("ICMS70");
                }
                if (jsonICMS.has("ICMS90")) {
                    jsonICMS90 = jsonICMS.getJSONObject("ICMS90");
                }
                if (jsonICMS.has("ICMSSN101")) {
                    jsonICMSSN101 = jsonICMS.getJSONObject("ICMSSN101");
                }
                if (jsonICMS.has("ICMSSN102")) {
                    jsonICMSSN102 = jsonICMS.getJSONObject("ICMSSN102");
                }
                if (jsonICMS.has("ICMSSN201")) {
                    jsonICMSSN201 = jsonICMS.getJSONObject("ICMSSN201");
                }
                if (jsonICMS.has("ICMSSN202")) {
                    jsonICMSSN202 = jsonICMS.getJSONObject("ICMSSN202");
                }
                if (jsonICMS.has("ICMSSN500")) {
                    jsonICMSSN500 = jsonICMS.getJSONObject("ICMSSN500");
                }
                if (jsonICMS.has("ICMSSN900")) {
                    jsonICMSSN900 = jsonICMS.getJSONObject("ICMSSN900");
                }
                
                int n = i + 1;

                Det det = new Det();

                // O numero do item.
                det.setNItem(Integer.toString(n));

                // Preenche os dados dos Produtos.
                Prod prod = new Prod();
                if (jsonProd.has("cProd")) {
                    prod.setCProd(jsonProd.get("cProd").toString());
                }
                if (jsonProd.has("cEAN")) {
                    prod.setCEAN(jsonProd.get("cEAN").toString());
                }
                if (webserviceAmbiente.equals("2")) {
                    prod.setXProd("NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL");
                } else {
                    prod.setXProd(jsonProd.get("xProd").toString());
                }
                if (jsonProd.has("NCM")) {
                    prod.setNCM(jsonProd.get("NCM").toString());
                }
                if (jsonProd.has("CEST")) {
                    prod.setCEST(jsonProd.get("CEST").toString());
                }
                if (jsonProd.has("indEscala")) {
                    prod.setIndEscala(jsonProd.get("indEscala").toString());
                }
                if (jsonProd.has("CNPJFab")) {
                    prod.setCNPJFab(jsonProd.get("CNPJFab").toString());
                }
                if (jsonProd.has("cBenef")) {
                    prod.setCBenef(jsonProd.get("cBenef").toString());
                }
                if (jsonProd.has("EXTIPI")) {
                    prod.setEXTIPI(jsonProd.get("EXTIPI").toString());
                }
                if (jsonProd.has("CFOP")) {
                    prod.setCFOP(jsonProd.get("CFOP").toString());
                }
                if (jsonProd.has("uCom")) {
                    prod.setUCom(jsonProd.get("uCom").toString());
                }
                if (jsonProd.has("qCom")) {
                    prod.setQCom(jsonProd.get("qCom").toString());
                }
                if (jsonProd.has("vUnCom")) {
                    prod.setVUnCom(jsonProd.get("vUnCom").toString());
                }
                if (jsonProd.has("vProd")) {
                    prod.setVProd(jsonProd.get("vProd").toString());
                }
                if (jsonProd.has("cEANTrib")) {
                    prod.setCEANTrib(jsonProd.get("cEANTrib").toString());
                }
                if (jsonProd.has("uTrib")) {
                    prod.setUTrib(jsonProd.get("uTrib").toString());
                }
                if (jsonProd.has("qTrib")) {
                    prod.setQTrib(jsonProd.get("qTrib").toString());
                }
                if (jsonProd.has("vUnTrib")) {
                    prod.setVUnTrib(jsonProd.get("vUnTrib").toString());
                }
                if (jsonProd.has("vFrete")) {
                    prod.setVFrete(jsonProd.get("vFrete").toString());
                }
                if (jsonProd.has("vSeg")) {
                    prod.setVSeg(jsonProd.get("vSeg").toString());
                }
                if (jsonProd.has("vDesc")) {
                    prod.setVDesc(jsonProd.get("vDesc").toString());
                }
                if (jsonProd.has("vOutro")) {
                    prod.setVOutro(jsonProd.get("vOutro").toString());
                }
                if (jsonProd.has("indTot")) {
                    prod.setIndTot(jsonProd.get("indTot").toString());
                }
                det.setProd(prod);

                // Preenche os dados do Imposto.
                Imposto imposto = new Imposto();
                
                ICMS icms = new ICMS();

                if (jsonICMS.has("ICMS00")) {
                    ICMS.ICMS00 icms00 = new ICMS.ICMS00();
                    icms00.setOrig(jsonICMS00.get("orig").toString());
                    icms00.setCST(jsonICMS00.get("CST").toString());
                    icms00.setModBC(jsonICMS00.get("modBC").toString());
                    icms00.setVBC(jsonICMS00.get("vBC").toString());
                    icms00.setPICMS(jsonICMS00.get("pICMS").toString());
                    icms00.setVICMS(jsonICMS00.get("vICMS").toString());
                    if (jsonICMS00.has("pFCP")) {
                        icms00.setPFCP(jsonICMS00.get("pFCP").toString());
                        icms00.setVFCP(jsonICMS00.get("vFCP").toString());
                    }
                    icms.setICMS00(icms00);
                }
                if (jsonICMS.has("ICMS10")) {
                    ICMS.ICMS10 icms10 = new ICMS.ICMS10();
                    icms10.setOrig(jsonICMS10.get("orig").toString());
                    icms10.setCST(jsonICMS10.get("CST").toString());
                    icms10.setModBC(jsonICMS10.get("modBC").toString());
                    icms10.setVBC(jsonICMS10.get("vBC").toString());
                    icms10.setPICMS(jsonICMS10.get("pICMS").toString());
                    icms10.setVICMS(jsonICMS10.get("vICMS").toString());
                    if (jsonICMS00.has("vBCFCP")) {
                        icms10.setVBCFCP(jsonICMS10.get("vBCFCP").toString());
                        icms10.setPFCP(jsonICMS10.get("pFCP").toString());
                        icms10.setVFCP(jsonICMS10.get("vFCP").toString());
                        icms10.setModBCST(jsonICMS10.get("modBCST").toString());
                        if (jsonICMS00.has("pMVAST")) {
                            icms10.setPMVAST(jsonICMS10.get("pMVAST").toString());
                        }
                        if (jsonICMS00.has("pRedBCST")) {
                            icms10.setPRedBCST(jsonICMS10.get("pRedBCST").toString());
                        }
                        icms10.setVBCST(jsonICMS10.get("vBCST").toString());
                        icms10.setPICMSST(jsonICMS10.get("pICMSST").toString());
                        icms10.setVICMSST(jsonICMS10.get("vICMSST").toString());
                    }
                    if (jsonICMS00.has("vBCFCPST")) {
                        icms10.setVBCFCPST(jsonICMS10.get("vBCFCPST").toString());
                        icms10.setPFCPST(jsonICMS10.get("pFCPST").toString());
                        icms10.setVFCPST(jsonICMS10.get("vFCPST").toString());
                    }
                    icms.setICMS10(icms10);
                }
                if (jsonICMS.has("ICMS20")) {
                    ICMS.ICMS20 icms20 = new ICMS.ICMS20();
                    icms20.setOrig(jsonICMS20.get("orig").toString());
                    icms20.setCST(jsonICMS20.get("CST").toString());
                    icms20.setModBC(jsonICMS20.get("modBC").toString());
                    icms20.setPRedBC(jsonICMS20.get("pRedBC").toString());
                    icms20.setVBC(jsonICMS20.get("vBC").toString());
                    icms20.setPICMS(jsonICMS20.get("pICMS").toString());
                    icms20.setVICMS(jsonICMS20.get("vICMS").toString());
                    if (jsonICMS20.has("vBCFCP")) {
                        icms20.setVBCFCP(jsonICMS20.get("vBCFCP").toString());
                        icms20.setPFCP(jsonICMS20.get("pFCP").toString());
                        icms20.setVFCP(jsonICMS20.get("vFCP").toString());
                    }
                    if (jsonICMS20.has("vICMSDeson")) {
                        icms20.setVICMSDeson(jsonICMS20.get("vICMSDeson").toString());
                        icms20.setMotDesICMS(jsonICMS20.get("motDesICMS").toString());
                    }
                    icms.setICMS20(icms20);
                }
                if (jsonICMS.has("ICMS30")) {
                    ICMS.ICMS30 icms30 = new ICMS.ICMS30();
                    icms30.setOrig(jsonICMS30.get("orig").toString());
                    icms30.setCST(jsonICMS30.get("CST").toString());
                    icms30.setModBCST(jsonICMS30.get("modBCST").toString());
                    if (jsonICMS30.has("pMVAST")) {
                        icms30.setPMVAST(jsonICMS30.get("pMVAST").toString());
                    }
                    if (jsonICMS30.has("pRedBCST")) {
                        icms30.setPRedBCST(jsonICMS30.get("pRedBCST").toString());
                    }
                    icms30.setVBCST(jsonICMS30.get("vBCST").toString());
                    icms30.setPICMSST(jsonICMS30.get("pICMSST").toString());
                    icms30.setVICMSST(jsonICMS30.get("vICMSST").toString());
                    if (jsonICMS30.has("vBCFCPST")) {
                        icms30.setVBCFCPST(jsonICMS30.get("vBCFCPST").toString());
                        icms30.setPFCPST(jsonICMS30.get("pFCPST").toString());
                        icms30.setVFCPST(jsonICMS30.get("vFCPST").toString());
                    }
                    if (jsonICMS30.has("vICMSDeson")) {
                        icms30.setVICMSDeson(jsonICMS30.get("vICMSDeson").toString());
                        icms30.setMotDesICMS(jsonICMS30.get("motDesICMS").toString());
                    }
                    icms.setICMS30(icms30);
                }
                if (jsonICMS.has("ICMS40")) {
                    ICMS.ICMS40 icms40 = new ICMS.ICMS40();
                    icms40.setOrig(jsonICMS40.get("orig").toString());
                    icms40.setCST(jsonICMS40.get("CST").toString());
                    if (jsonICMS40.has("vICMSDeson")) {
                        icms40.setVICMSDeson(jsonICMS40.get("vICMSDeson").toString());
                        icms40.setMotDesICMS(jsonICMS40.get("motDesICMS").toString());
                    }
                    icms.setICMS40(icms40);
                }
                if (jsonICMS.has("ICMS51")) {
                    ICMS.ICMS51 icms51 = new ICMS.ICMS51();
                    icms51.setOrig(jsonICMS51.get("orig").toString());
                    icms51.setCST(jsonICMS51.get("CST").toString());
                    if (jsonICMS51.has("modBC")) {
                        icms51.setModBC(jsonICMS51.get("modBC").toString());
                    }
                    if (jsonICMS51.has("pRedBC")) {
                        icms51.setPRedBC(jsonICMS51.get("pRedBC").toString());
                    }
                    if (jsonICMS51.has("vBC")) {
                        icms51.setVBC(jsonICMS51.get("vBC").toString());
                    }
                    if (jsonICMS51.has("pICMS")) {
                        icms51.setPICMS(jsonICMS51.get("pICMS").toString());
                    }
                    if (jsonICMS51.has("vICMSOp")) {
                        icms51.setPICMS(jsonICMS51.get("vICMSOp").toString());
                    }
                    if (jsonICMS51.has("pDif")) {
                        icms51.setPICMS(jsonICMS51.get("pDif").toString());
                    }
                    if (jsonICMS51.has("vICMSDif")) {
                        icms51.setPICMS(jsonICMS51.get("vICMSDif").toString());
                    }
                    if (jsonICMS51.has("vICMS")) {
                        icms51.setVICMS(jsonICMS51.get("vICMS").toString());
                    }
                    if (jsonICMS51.has("vBCFCP")) {
                        icms51.setVBCFCP(jsonICMS51.get("vBCFCP").toString());
                        icms51.setPFCP(jsonICMS51.get("pFCP").toString());
                        icms51.setVFCP(jsonICMS51.get("vFCP").toString());
                    }
                    icms.setICMS51(icms51);
                }
                if (jsonICMS.has("ICMS60")) {
                    ICMS.ICMS60 icms60 = new ICMS.ICMS60();
                    icms60.setOrig(jsonICMS60.get("orig").toString());
                    icms60.setCST(jsonICMS60.get("CST").toString());
                    if (jsonICMS60.has("vBCSTRet")) {
                        icms60.setVBCSTRet(jsonICMS60.get("vBCSTRet").toString());
                        icms60.setPST(jsonICMS60.get("pST").toString());
                        if (jsonICMS60.has("vICMSSubstituto")) {
                            icms60.setVICMSSubstituto(jsonICMS60.get("vICMSSubstituto").toString());
                        }
                        icms60.setVICMSSTRet(jsonICMS60.get("vICMSSTRet").toString());
                    }
                    if (jsonICMS60.has("vBCFCPSTRet")) {
                        icms60.setVBCFCPSTRet(jsonICMS60.get("vBCFCPSTRet").toString());
                        icms60.setPFCPSTRet(jsonICMS60.get("pFCPSTRet").toString());
                        icms60.setVFCPSTRet(jsonICMS60.get("vFCPSTRet").toString());
                    }
                    if (jsonICMS60.has("pRedBCEfet")) {
                        icms60.setPRedBCEfet(jsonICMS60.get("pRedBCEfet").toString());
                        icms60.setVBCEfet(jsonICMS60.get("vBCEfet").toString());
                        icms60.setPICMSEfet(jsonICMS60.get("pICMSEfet").toString());
                        icms60.setVICMSEfet(jsonICMS60.get("vICMSEfet").toString());
                    }
                    icms.setICMS60(icms60);
                }
                if (jsonICMS.has("ICMS70")) {
                    ICMS.ICMS70 icms70 = new ICMS.ICMS70();
                    icms70.setOrig(jsonICMS70.get("orig").toString());
                    icms70.setCST(jsonICMS70.get("CST").toString());
                    icms70.setModBC(jsonICMS70.get("modBC").toString());
                    icms70.setPRedBC(jsonICMS70.get("pRedBC").toString());
                    icms70.setVBC(jsonICMS70.get("vBC").toString());
                    icms70.setPICMS(jsonICMS70.get("pICMS").toString());
                    icms70.setVICMS(jsonICMS70.get("vICMS").toString());
                    if (jsonICMS70.has("vBCFCP")) {
                        icms70.setVBCFCP(jsonICMS70.get("vBCFCP").toString());
                        icms70.setPFCP(jsonICMS70.get("pFCP").toString());
                        icms70.setVFCP(jsonICMS70.get("vFCP").toString());
                        icms70.setModBCST(jsonICMS70.get("modBCST").toString());
                        if (jsonICMS70.has("pMVAST")) {
                            icms70.setPMVAST(jsonICMS70.get("pMVAST").toString());
                        }
                        if (jsonICMS70.has("pRedBCST")) {
                            icms70.setPRedBCST(jsonICMS70.get("pRedBCST").toString());
                        }
                        icms70.setVBCST(jsonICMS70.get("vBCST").toString());
                        icms70.setPICMSST(jsonICMS70.get("pICMSST").toString());
                        icms70.setVICMSST(jsonICMS70.get("vICMSST").toString());
                    }
                    if (jsonICMS70.has("vBCFCPST")) {
                        icms70.setVBCFCPST(jsonICMS70.get("vBCFCPST").toString());
                        icms70.setPFCPST(jsonICMS70.get("pFCPST").toString());
                        icms70.setVFCPST(jsonICMS70.get("vFCPST").toString());
                    }
                    if (jsonICMS70.has("vICMSDeson")) {
                        icms70.setVICMSDeson(jsonICMS70.get("vICMSDeson").toString());
                        icms70.setMotDesICMS(jsonICMS70.get("motDesICMS").toString());
                    }
                    icms.setICMS70(icms70);
                }
                if (jsonICMS.has("ICMS90")) {
                    ICMS.ICMS90 icms90 = new ICMS.ICMS90();
                    icms90.setOrig(jsonICMS90.get("orig").toString());
                    icms90.setCST(jsonICMS90.get("CST").toString());
                    if (jsonICMS90.has("modBC")) {
                        icms90.setModBC(jsonICMS90.get("modBC").toString());
                        icms90.setVBC(jsonICMS90.get("vBC").toString());
                        if (jsonICMS90.has("pRedBC")) {
                            icms90.setPRedBC(jsonICMS90.get("pRedBC").toString());
                        }
                        icms90.setPICMS(jsonICMS90.get("pICMS").toString());
                        icms90.setVICMS(jsonICMS90.get("vICMS").toString());
                    }
                    if (jsonICMS90.has("vBCFCP")) {
                        icms90.setVBCFCP(jsonICMS90.get("vBCFCP").toString());
                        icms90.setPFCP(jsonICMS90.get("pFCP").toString());
                        icms90.setVFCP(jsonICMS90.get("vFCP").toString());
                    }
                    if (jsonICMS90.has("modBCST")) {
                        icms90.setModBCST(jsonICMS90.get("modBCST").toString());
                        if (jsonICMS90.has("pMVAST")) {
                            icms90.setPMVAST(jsonICMS90.get("pMVAST").toString());
                        }
                        if (jsonICMS90.has("pRedBCST")) {
                            icms90.setPRedBCST(jsonICMS90.get("pRedBCST").toString());
                        }
                        icms90.setVBCST(jsonICMS90.get("vBCST").toString());
                        icms90.setPICMSST(jsonICMS90.get("pICMSST").toString());
                        icms90.setVICMSST(jsonICMS90.get("vICMSST").toString());
                    }
                    if (jsonICMS90.has("vBCFCPST")) {
                        icms90.setVBCFCPST(jsonICMS90.get("vBCFCPST").toString());
                        icms90.setPFCPST(jsonICMS90.get("pFCPST").toString());
                        icms90.setVFCPST(jsonICMS90.get("vFCPST").toString());
                    }
                    if (jsonICMS90.has("vICMSDeson")) {
                        icms90.setVICMSDeson(jsonICMS90.get("vICMSDeson").toString());
                        icms90.setMotDesICMS(jsonICMS90.get("motDesICMS").toString());
                    }
                    icms.setICMS90(icms90);
                }
                if (jsonICMS.has("ICMSSN101")) {
                    ICMS.ICMSSN101 icmsSN101 = new ICMS.ICMSSN101();
                    icmsSN101.setOrig(jsonICMSSN101.get("orig").toString());
                    icmsSN101.setCSOSN(jsonICMSSN101.get("CSOSN").toString());
                    icmsSN101.setPCredSN(jsonICMSSN101.get("pCredSN").toString());
                    icmsSN101.setVCredICMSSN(jsonICMSSN101.get("vCredICMSSN").toString());
                    icms.setICMSSN101(icmsSN101);
                }
                if (jsonICMS.has("ICMSSN102")) {
                    ICMS.ICMSSN102 icmsSN102 = new ICMS.ICMSSN102();
                    icmsSN102.setOrig(jsonICMSSN102.get("orig").toString());
                    icmsSN102.setCSOSN(jsonICMSSN102.get("CSOSN").toString());
                    icms.setICMSSN102(icmsSN102);
                }
                if (jsonICMS.has("ICMSSN201")) {
                    ICMS.ICMSSN201 icmsSN201 = new ICMS.ICMSSN201();
                    icmsSN201.setOrig(jsonICMSSN201.get("orig").toString());
                    icmsSN201.setCSOSN(jsonICMSSN201.get("CSOSN").toString());
                    icmsSN201.setModBCST(jsonICMSSN201.get("modBCST").toString());
                    if (jsonICMSSN201.has("pMVAST")) {
                        icmsSN201.setPMVAST(jsonICMSSN201.get("pMVAST").toString());
                    }
                    if (jsonICMSSN201.has("pRedBCST")) {
                        icmsSN201.setPRedBCST(jsonICMSSN201.get("pRedBCST").toString());
                    }
                    icmsSN201.setVBCST(jsonICMSSN201.get("vBCST").toString());
                    icmsSN201.setPICMSST(jsonICMSSN201.get("pICMSST").toString());
                    icmsSN201.setVICMSST(jsonICMSSN201.get("vICMSST").toString());
                    if (jsonICMSSN201.has("vBCFCPST")) {
                        icmsSN201.setVBCFCPST(jsonICMSSN201.get("vBCFCPST").toString());
                        icmsSN201.setPFCPST(jsonICMSSN201.get("pFCPST").toString());
                        icmsSN201.setVFCPST(jsonICMSSN201.get("vFCPST").toString());
                        icmsSN201.setPCredSN(jsonICMSSN201.get("pCredSN").toString());
                        icmsSN201.setVCredICMSSN(jsonICMSSN201.get("vCredICMSSN").toString());
                    }
                    icms.setICMSSN201(icmsSN201);
                }
                if (jsonICMS.has("ICMSSN202")) {
                    ICMS.ICMSSN202 icmsSN202 = new ICMS.ICMSSN202();
                    icmsSN202.setOrig(jsonICMSSN202.get("orig").toString());
                    icmsSN202.setCSOSN(jsonICMSSN202.get("CSOSN").toString());
                    icmsSN202.setModBCST(jsonICMSSN202.get("modBCST").toString());
                    if (jsonICMSSN202.has("pMVAST")) {
                        icmsSN202.setPMVAST(jsonICMSSN202.get("pMVAST").toString());
                    }
                    if (jsonICMSSN202.has("pRedBCST")) {
                        icmsSN202.setPRedBCST(jsonICMSSN202.get("pRedBCST").toString());
                    }
                    icmsSN202.setVBCST(jsonICMSSN202.get("vBCST").toString());
                    icmsSN202.setPICMSST(jsonICMSSN202.get("pICMSST").toString());
                    icmsSN202.setVICMSST(jsonICMSSN202.get("vICMSST").toString());
                    if (jsonICMSSN202.has("vBCFCPST")) {
                        icmsSN202.setVBCFCPST(jsonICMSSN202.get("vBCFCPST").toString());
                        icmsSN202.setPFCPST(jsonICMSSN202.get("pFCPST").toString());
                        icmsSN202.setVFCPST(jsonICMSSN202.get("vFCPST").toString());
                    }
                    icms.setICMSSN202(icmsSN202);
                }
                if (jsonICMS.has("ICMSSN500")) {
                    ICMS.ICMSSN500 icmsSN500 = new ICMS.ICMSSN500();
                    icmsSN500.setOrig(jsonICMSSN500.get("orig").toString());
                    icmsSN500.setCSOSN(jsonICMSSN500.get("CSOSN").toString());
                    if (jsonICMSSN500.has("vBCSTRet")) {
                        icmsSN500.setVBCSTRet(jsonICMSSN500.get("vBCSTRet").toString());
                        icmsSN500.setPST(jsonICMSSN500.get("pST").toString());
                        if (jsonICMSSN500.has("vICMSSubstituto")) {
                            icmsSN500.setVICMSSubstituto(jsonICMSSN500.get("vICMSSubstituto").toString());
                        }
                        icmsSN500.setVICMSSTRet(jsonICMSSN500.get("vICMSSTRet").toString());
                    }
                    if (jsonICMSSN500.has("vBCFCPSTRet")) {
                        icmsSN500.setVBCFCPSTRet(jsonICMSSN500.get("vBCFCPSTRet").toString());
                        icmsSN500.setPFCPSTRet(jsonICMSSN500.get("pFCPSTRet").toString());
                        icmsSN500.setVFCPSTRet(jsonICMSSN500.get("vFCPSTRet").toString());
                    }
                    if (jsonICMSSN500.has("pRedBCEfet")) {
                        icmsSN500.setPRedBCEfet(jsonICMSSN500.get("pRedBCEfet").toString());
                        icmsSN500.setVBCEfet(jsonICMSSN500.get("vBCEfet").toString());
                        icmsSN500.setPICMSEfet(jsonICMSSN500.get("pICMSEfet").toString());
                        icmsSN500.setVICMSEfet(jsonICMSSN500.get("vICMSEfet").toString());
                    }
                    icms.setICMSSN500(icmsSN500);
                }
                if (jsonICMS.has("ICMSSN900")) {
                    ICMS.ICMSSN900 icmsSN900 = new ICMS.ICMSSN900();
                    icmsSN900.setOrig(jsonICMSSN900.get("orig").toString());
                    icmsSN900.setCSOSN(jsonICMSSN900.get("CSOSN").toString());
                    if (jsonICMSSN900.has("modBC")) {
                        icmsSN900.setModBC(jsonICMSSN900.get("modBC").toString());
                        icmsSN900.setVBC(jsonICMSSN900.get("vBC").toString());
                        if (jsonICMSSN900.has("pRedBC")) {
                            icmsSN900.setPRedBC(jsonICMSSN900.get("pRedBC").toString());
                        }
                        icmsSN900.setPICMS(jsonICMSSN900.get("pICMS").toString());
                        icmsSN900.setVICMS(jsonICMSSN900.get("vICMS").toString());
                    }
                    if (jsonICMSSN900.has("modBCST")) {
                        icmsSN900.setModBCST(jsonICMSSN900.get("modBCST").toString());
                        if (jsonICMSSN900.has("pMVAST")) {
                            icmsSN900.setPMVAST(jsonICMSSN900.get("pMVAST").toString());
                        }
                        if (jsonICMSSN900.has("pRedBCST")) {
                            icmsSN900.setPRedBCST(jsonICMSSN900.get("pRedBCST").toString());
                        }
                        icmsSN900.setVBCST(jsonICMSSN900.get("vBCST").toString());
                        icmsSN900.setPICMSST(jsonICMSSN900.get("pICMSST").toString());
                        icmsSN900.setVICMSST(jsonICMSSN900.get("vICMSST").toString());
                    }
                    if (jsonICMSSN900.has("vBCFCPST")) {
                        icmsSN900.setVBCFCPST(jsonICMSSN900.get("vBCFCPST").toString());
                        icmsSN900.setPFCPST(jsonICMSSN900.get("pFCPST").toString());
                        icmsSN900.setVFCPST(jsonICMSSN900.get("vFCPST").toString());
                    }
                    if (jsonICMSSN900.has("pCredSN")) {
                        icmsSN900.setPCredSN(jsonICMSSN900.get("pCredSN").toString());
                        icmsSN900.setVCredICMSSN(jsonICMSSN900.get("vCredICMSSN").toString());
                    }
                    icms.setICMSSN900(icmsSN900);
                }
                
                String vTotTrib = jsonImposto.get("vTotTrib").toString();
                
                imposto.getContent().add(new ObjectFactory().createTNFeInfNFeDetImpostoVTotTrib(vTotTrib)); 
                
                JAXBElement<ICMS> icmsElement = new JAXBElement<ICMS>(new QName("ICMS"), ICMS.class, icms);
                imposto.getContent().add(icmsElement);

                if (jsonImposto.has("PIS")) {
                    JSONObject jsonPIS = jsonImposto.getJSONObject("PIS");
                    JSONObject jsonPISAliq = jsonPIS.getJSONObject("PISAliq");

                    PIS pis = new PIS();
                    PISAliq pisAliq = new PISAliq();
                    pisAliq.setCST(jsonPISAliq.get("CST").toString());
                    pisAliq.setVBC(jsonPISAliq.get("vBC").toString());
                    pisAliq.setPPIS(jsonPISAliq.get("pPIS").toString());
                    pisAliq.setVPIS(jsonPISAliq.get("vPIS").toString());
                    pis.setPISAliq(pisAliq);
                    
                    JAXBElement<PIS> pisElement = new JAXBElement<PIS>(new QName("PIS"), PIS.class, pis);
                    imposto.getContent().add(pisElement);
                }
                
                if (jsonImposto.has("COFINS")) {
                    JSONObject jsonCOFINS = jsonImposto.getJSONObject("COFINS");
                    JSONObject jsonCOFINSAliq = jsonCOFINS.getJSONObject("COFINSAliq");

                    COFINS cofins = new COFINS();
                    COFINSAliq cofinsAliq = new COFINSAliq();
                    cofinsAliq.setCST(jsonCOFINSAliq.get("CST").toString());
                    cofinsAliq.setVBC(jsonCOFINSAliq.get("vBC").toString());
                    cofinsAliq.setPCOFINS(jsonCOFINSAliq.get("pCOFINS").toString());
                    cofinsAliq.setVCOFINS(jsonCOFINSAliq.get("vCOFINS").toString());
                    cofins.setCOFINSAliq(cofinsAliq);
                    
                    JAXBElement<COFINS> cofinsElement = new JAXBElement<COFINS>(new QName("COFINS"), COFINS.class, cofins);
                    imposto.getContent().add(cofinsElement);
                }
                
                det.setImposto(imposto);

                infNFe.getDet().addAll(Collections.singletonList(det));
            }

            // Preenche os totais da NFC-e.
            Total total = new Total();
            ICMSTot icmstot = new ICMSTot();
            icmstot.setVBC(jsonICMSTot.get("vBC").toString());
            icmstot.setVICMS(jsonICMSTot.get("vICMS").toString());
            icmstot.setVICMSDeson(jsonICMSTot.get("vICMSDeson").toString());
            if (jsonICMSTot.has("vFCPUFDest")) {
                icmstot.setVFCPUFDest(jsonICMSTot.get("vFCPUFDest").toString());
            }
            if (jsonICMSTot.has("vICMSUFDest")) {
                icmstot.setVICMSUFDest(jsonICMSTot.get("vICMSUFDest").toString());
            }
            if (jsonICMSTot.has("vICMSUFRemet")) {
                icmstot.setVICMSUFRemet(jsonICMSTot.get("vICMSUFRemet").toString());
            }
            icmstot.setVFCP(jsonICMSTot.get("vFCP").toString());
            icmstot.setVBCST(jsonICMSTot.get("vFCPST").toString());
            icmstot.setVST(jsonICMSTot.get("vFCPSTRet").toString());
            icmstot.setVFCPST(jsonICMSTot.get("vBCST").toString());
            icmstot.setVFCPSTRet(jsonICMSTot.get("vST").toString());
            icmstot.setVProd(jsonICMSTot.get("vProd").toString());
            icmstot.setVFrete(jsonICMSTot.get("vFrete").toString());
            icmstot.setVSeg(jsonICMSTot.get("vSeg").toString());
            icmstot.setVDesc(jsonICMSTot.get("vDesc").toString());
            icmstot.setVII(jsonICMSTot.get("vII").toString());
            icmstot.setVIPI(jsonICMSTot.get("vIPI").toString());
            icmstot.setVIPIDevol(jsonICMSTot.get("vIPIDevol").toString());
            icmstot.setVPIS(jsonICMSTot.get("vPIS").toString());
            icmstot.setVCOFINS(jsonICMSTot.get("vCOFINS").toString());
            icmstot.setVOutro(jsonICMSTot.get("vOutro").toString());
            icmstot.setVNF(jsonICMSTot.get("vNF").toString());
            if (jsonICMSTot.has("vTotTrib")) {
                icmstot.setVTotTrib(jsonICMSTot.get("vTotTrib").toString());
            }
            total.setICMSTot(icmstot);

            infNFe.setTotal(total);

            // Preenche os dados do Transporte.
            Transp transp = new Transp();
            transp.setModFrete(jsonTransp.get("modFrete").toString());

            infNFe.setTransp(transp);

            // Preenche dados dos Pagamentos.
            Pag pag = new Pag();

            for (int i = 0; i < jsonDetPag.length(); i++) {
                JSONObject jsonItemDetPag = jsonDetPag.getJSONObject(i);
                Pag.DetPag detPag = new Pag.DetPag();
                if (jsonItemDetPag.has("indPag")) {
                    detPag.setIndPag(jsonItemDetPag.get("indPag").toString());
                }
                detPag.setTPag(jsonItemDetPag.get("tPag").toString());
                detPag.setVPag(jsonItemDetPag.get("vPag").toString());
                if (jsonItemDetPag.has("card")) {
                    JSONObject jsonCard = jsonItemDetPag.getJSONObject("card");

                    Pag.DetPag.Card card = new Pag.DetPag.Card();
                    card.setCNPJ(jsonCard.get("CNPJ").toString());
                    card.setTpIntegra(jsonCard.get("tpIntegra").toString());
                    card.setTBand(jsonCard.get("tBand").toString());
                    card.setCAut(jsonCard.get("cAut").toString());
                    detPag.setCard(card);
                }
                pag.getDetPag().add(detPag);
            }
            if (jsonPag.has("vTroco")) {
                pag.setVTroco(jsonPag.get("vTroco").toString());
            }
            infNFe.setPag(pag);
            
            if (jsonInfNFe.has("infAdic")) {
                JSONObject jsonInfAdic = jsonInfNFe.getJSONObject("infAdic");

                InfAdic infAdic = new InfAdic();
                infAdic.setInfCpl(jsonInfAdic.get("infCpl").toString());
                infNFe.setInfAdic(infAdic);
            }

            TNFe nfe = new TNFe();
            nfe.setInfNFe(infNFe);

            // Monta a EnviNfe.
            TEnviNFe enviNFe = new TEnviNFe();
            enviNFe.setVersao(ConstantesUtil.VERSAO.NFE);
            enviNFe.setIdLote("1");
            enviNFe.setIndSinc("1");
            enviNFe.getNFe().add(nfe);
            
            // Monta a EnviNfe para contingência pois após assinada não podemos mais modificar a EnviNfe.
            TEnviNFe enviNFeContingencia = enviNFe;
            
            try {
                // Monta e Assina o XML.
                enviNFe = Nfe.montaNfe(config, enviNFe, true);

                // Monta o QR Code.
                urlChave = WebServiceUtil.getUrl(config, DocumentoEnum.NFCE, ServicosEnum.URL_CONSULTANFCE);  
                if (tpEmis.equals("9")) {
                    qrCode = preencheQRCodeContingencia(enviNFe, config, idToken, csc);
                } else {
                    qrCode = preencheQRCode(enviNFe, config, idToken, csc);
                }
                TNFe.InfNFeSupl infNFeSupl = new TNFe.InfNFeSupl();
                infNFeSupl.setQrCode(qrCode);
                infNFeSupl.setUrlChave(urlChave);
                enviNFe.getNFe().get(0).setInfNFeSupl(infNFeSupl);
                
                // Simula contingência sem autorização da NFC-e.
                if (simularContingencia.equals("1")) {
                    dhRecbto = "";
                    nProt = "";
                    tpAmb = "";
                    tpEmis = "";
                    dhCont = "";
                    xJust = "";
                    chave = "";
                    urlChave = "";
                    qrCode = "";
                    xml = "";
                    
                    throw new Exception("Connection reset");
                }
                
                // Envia a NFC-e para a SEFAZ.
                TRetEnviNFe retorno = Nfe.enviarNfe(config, enviNFe, DocumentoEnum.NFCE);
                
                // VErifica se o retorno é assíncrono.
                if (RetornoUtil.isRetornoAssincrono(retorno)) {
                    // Obtém o Recibo.
                    String recibo = retorno.getInfRec().getNRec();
                    int tentativa = 0;
                    TRetConsReciNFe retornoNfe = null;

                    // Realiza a consulta diversas vezes.
                    while (tentativa < 10) {
                        retornoNfe = Nfe.consultaRecibo(config, recibo, DocumentoEnum.NFCE);
                        if (retornoNfe.getCStat().equals(StatusEnum.LOTE_EM_PROCESSAMENTO.getCodigo())) {
                            System.out.println("INFO: Lote Em Processamento, vai tentar novamente apos 1 Segundo.");
                            Thread.sleep(1000);
                            tentativa++;
                        } else {
                            break;
                        }
                    }

                    RetornoUtil.validaAssincrono(retornoNfe);
                    
                    cStat = retornoNfe.getProtNFe().get(0).getInfProt().getCStat();
                    xMotivo = retornoNfe.getProtNFe().get(0).getInfProt().getXMotivo();
                    
                    System.out.println("cStat: " + cStat + " - " + xMotivo);
                    if (cStat.equals("100")) {
                        nProt = retornoNfe.getProtNFe().get(0).getInfProt().getNProt();
                        dhRecbto = retornoNfe.getProtNFe().get(0).getInfProt().getDhRecbto();
                        xml = XmlNfeUtil.criaNfeProc(enviNFe, retornoNfe.getProtNFe().get(0));
                        
                        // Salva o XML da NFC-e.
                        FileWriter writer = new FileWriter(caminhoXML + "/" + chave + ".xml");
                        writer.write(xml);
                        writer.close();
                        
                        // Imprime a NF-e.
                        if (imprimirDANFE.equals("1")) {
                            JSONObject jsonInfProt = new JSONObject();
                            jsonInfProt.put("tpAmb", tpAmb);
                            jsonInfProt.put("tpEmis", tpEmis);
                            jsonInfProt.put("chNFe", chave.substring(3));
                            jsonInfProt.put("dhRecbto", dhRecbto);
                            jsonInfProt.put("nProt", nProt);
                            jsonInfProt.put("urlChave", urlChave);
                            jsonInfProt.put("qrCode", qrCode);
                            jsonInfProt.put("cStat", cStat);
                            jsonInfProt.put("xMotivo", xMotivo);
                            json.put("infProt", jsonInfProt);
                            
                            imprimeDANFE(json, nomeImpressora, numeroColunas);
                        }
                        
                        System.out.println("Protocolo: " + nProt);
                        System.out.println("XML Final: " + xml);
                    }
                } else {
                    // Retorno síncrono.
                    RetornoUtil.validaSincrono(retorno);
                    
                    cStat = retorno.getProtNFe().getInfProt().getCStat();
                    xMotivo = retorno.getProtNFe().getInfProt().getXMotivo();
                    
                    System.out.println("cStat: " + cStat + " - " + xMotivo);
                    if (cStat.equals("100")) {
                        nProt = retorno.getProtNFe().getInfProt().getNProt();
                        dhRecbto = retorno.getProtNFe().getInfProt().getDhRecbto();
                        xml = XmlNfeUtil.criaNfeProc(enviNFe, retorno.getProtNFe());
                        
                        // Salva o XML da NFC-e.
                        FileWriter writer = new FileWriter(caminhoXML + "/" + chave + ".xml");
                        writer.write(xml);
                        writer.close();
                        
                        // Imprime a NF-e.
                        if (imprimirDANFE.equals("1")) {
                            JSONObject jsonInfProt = new JSONObject();
                            jsonInfProt.put("tpAmb", tpAmb);
                            jsonInfProt.put("tpEmis", tpEmis);
                            jsonInfProt.put("chNFe", chave.substring(3));
                            jsonInfProt.put("dhRecbto", dhRecbto);
                            jsonInfProt.put("nProt", nProt);
                            jsonInfProt.put("urlChave", urlChave);
                            jsonInfProt.put("qrCode", qrCode);
                            jsonInfProt.put("cStat", cStat);
                            jsonInfProt.put("xMotivo", xMotivo);
                            json.put("infProt", jsonInfProt);
                            
                            imprimeDANFE(json, nomeImpressora, numeroColunas);
                        }
                        
                        System.out.println("Protocolo: " + nProt);
                        System.out.println("XML Final: " + xml);
                    }
                }
                
                // Simula contingência com autorização da NFC-e.
                if (simularContingencia.equals("2")) {
                    dhRecbto = "";
                    nProt = "";
                    tpAmb = "";
                    tpEmis = "";
                    dhCont = "";
                    xJust = "";
                    chave = "";
                    urlChave = "";
                    qrCode = "";
                    xml = "";
                    
                    throw new Exception("Connection reset");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                if (xMotivo.indexOf("Rejeicao") > -1) {
                    cStat = xMotivo.substring(0, 3);
                }
                
                // Se o erro não ocorreu por rejeição, cria o XML da NF-e em contingência.
                if (cStat.equals("000")) {
                    // Tipo da emissão contingência: tpEmis = "9".
                    tpAmb = config.getAmbiente().getCodigo();
                    tpEmis = "9";
                    chaveUtil = new ChaveUtil(config.getEstado(), cnpj, modelo, serie, numeroNFCe, tpEmis, cnf, dataEmissao);
                    chave = chaveUtil.getChaveNF();
                    cdv = chaveUtil.getDigitoVerificador();
                    enviNFeContingencia.getNFe().get(0).getInfNFe().setId(chave);
                    enviNFeContingencia.getNFe().get(0).getInfNFe().getIde().setTpEmis(tpEmis);
                    enviNFeContingencia.getNFe().get(0).getInfNFe().getIde().setCDV(cdv);

                    dhCont = XmlNfeUtil.dataNfe(dataEmissao);
                    xJust = "Erro ao tentar enviar o XML da NF-e para a SEFAZ";
                    enviNFeContingencia.getNFe().get(0).getInfNFe().getIde().setDhCont(dhCont);
                    enviNFeContingencia.getNFe().get(0).getInfNFe().getIde().setXJust(xJust);

                    try {
                        enviNFeContingencia = Nfe.montaNfe(config, enviNFeContingencia, true);

                        urlChave = WebServiceUtil.getUrl(config, DocumentoEnum.NFCE, ServicosEnum.URL_CONSULTANFCE);  
                        qrCode = preencheQRCodeContingencia(enviNFeContingencia, config, idToken, csc);
                        TNFe.InfNFeSupl infNFeSupl = new TNFe.InfNFeSupl();
                        infNFeSupl.setQrCode(qrCode);
                        infNFeSupl.setUrlChave(urlChave);
                        enviNFeContingencia.getNFe().get(0).setInfNFeSupl(infNFeSupl);

                        xml = XmlNfeUtil.objectToXml(enviNFeContingencia.getNFe().get(0), config.getEncode());

                        System.out.println("XML Final: " + xml);

                        // Salva o XML da NFC-e.
                        FileWriter writer = new FileWriter(caminhoXML + "/" + chave + ".xml");
                        writer.write(xml);
                        writer.close();
                        
                        // Imprime a NF-e.
                        if (imprimirDANFE.equals("1")) {
                            JSONObject jsonInfProt = new JSONObject();
                            jsonInfProt.put("tpAmb", tpAmb);
                            jsonInfProt.put("tpEmis", tpEmis);
                            jsonInfProt.put("chNFe", chave.substring(3));
                            jsonInfProt.put("dhRecbto", dhRecbto);
                            jsonInfProt.put("nProt", nProt);
                            jsonInfProt.put("urlChave", urlChave);
                            jsonInfProt.put("qrCode", qrCode);
                            jsonInfProt.put("cStat", cStat);
                            jsonInfProt.put("xMotivo", xMotivo);
                            json.put("infProt", jsonInfProt);
                            
                            imprimeDANFE(json, nomeImpressora, numeroColunas);
                        }
                    } catch (Exception error) {
                        System.out.println(error.getMessage());

                        cStat = "000";
                        xMotivo = error.getMessage();
                    }
                } else {
                    dhRecbto = "";
                    nProt = "";
                    tpAmb = "";
                    tpEmis = "";
                    dhCont = "";
                    xJust = "";
                    chave = "";
                    urlChave = "";
                    qrCode = "";
                    xml = "";
                }
            }
            
            JSONObject responseJSON = new JSONObject();
            responseJSON.put("cStat", cStat);
            responseJSON.put("xMotivo", xMotivo);
            responseJSON.put("dhRecbto", dhRecbto);
            responseJSON.put("nProt", nProt);
            responseJSON.put("tpAmb", tpAmb);
            responseJSON.put("tpEmis", tpEmis);
            responseJSON.put("dhCont", dhCont);
            responseJSON.put("xJust", xJust);
            if (chave.length() > 0) {
                responseJSON.put("chave", chave.substring(3));
            }
            responseJSON.put("urlChave", urlChave);
            responseJSON.put("qrCode", qrCode);
            responseJSON.put("xml", xml);

            System.out.println(responseJSON.toString());
            
            String response = responseJSON.toString();
            
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            httpExchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }
    
    /**
     * Processa o serviço NFeConsulta.
     */
    static class NFeConsultaHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String caminhoNFCeMonitor = System.getProperty("user.dir");
        
            String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
            String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);

            JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
            
            InputStream inputStream = httpExchange.getRequestBody(); 
            Scanner scanner = new Scanner(inputStream);
            String linha;
            String dados = new String();
            
            while (scanner.hasNextLine()) {
                linha = scanner.nextLine();
                dados = dados + linha;
            }
            
            JSONObject json = new JSONObject(dados);
            
            String cStat = "";
            String xMotivo = "";
            
            /*
             * Prepara o XML da NFC-e.
             */
            
            // Inicia As configurações.
            ConfiguracoesNfe config;
            
            try {
                config = Config.iniciaConfiguracoes();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
                
                return;
            }
            
            JSONObject jsonCconsSitNFe = json.getJSONObject("consSitNFe");
            
            String chNFe = jsonCconsSitNFe.get("chNFe").toString();
            
            try {
                // Envia a consulta.
                TRetConsSitNFe retorno = Nfe.consultaXml(config, chNFe, DocumentoEnum.NFCE);
                
                cStat = retorno.getCStat();
                xMotivo = retorno.getXMotivo();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }
    }
    
    /**
     * Processa o serviço NFeCartaCorrecao.
     */
    static class NFeCartaCorrecaoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String caminhoNFCeMonitor = System.getProperty("user.dir");
        
            String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
            String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);

            JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
            
            InputStream inputStream = httpExchange.getRequestBody(); 
            Scanner scanner = new Scanner(inputStream);
            String linha;
            String dados = new String();
            
            while (scanner.hasNextLine()) {
                linha = scanner.nextLine();
                dados = dados + linha;
            }
            
            JSONObject json = new JSONObject(dados);
            
            String cStat = "";
            String xMotivo = "";
            String nProt = "";
            String xml = "";
            
            /*
             * Prepara o XML da NFC-e.
             */
            
            // Inicia As configurações.
            ConfiguracoesNfe config;
            
            try {
                config = Config.iniciaConfiguracoes();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                responseJSON.put("nProt", nProt);
                responseJSON.put("xml", xml);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
                
                return;
            }
            
            JSONObject jsonInfEvento = json.getJSONObject("infEvento");
            
            String cnpj = jsonInfEvento.get("CNPJ").toString();
            String chNFe = jsonInfEvento.get("chNFe").toString();
            String nSeqEvento = jsonInfEvento.get("nSeqEvento").toString();
            String descEvento = jsonInfEvento.get("descEvento").toString();
            
            int sequenciaEvento = Integer.parseInt(nSeqEvento);
            
            // Cria um evento para a Carta de Correção.
            Evento cce = new Evento();
            // CNPJ do emitente.
            cce.setCnpj(cnpj);
            // Chave da NF-e.
            cce.setChave(chNFe);
            // Data da Carta de Correção.
            cce.setDataEvento(LocalDateTime.now());
            // Sequência do evento. Podem ser feitas até 20 correções.
            cce.setSequencia(sequenciaEvento);
            // Texto da Carta de Correção.
            cce.setMotivo(descEvento);
            
            try {
                // Monta o Evento.
                TEnvEvento envEvento = CartaCorrecaoUtil.montaCCe(cce,config);

                // Envia a Carta de Correção.
                TRetEnvEvento retorno = Nfe.cce(config, envEvento, true);

                // Valida o retorno.
                RetornoUtil.validaCartaCorrecao(retorno);
                
                // Obtém o protocolo da Carta Correção.
                JSONObject responseJSON = new JSONObject();
                
                retorno.getRetEvento().forEach(resultado -> {
                    responseJSON.put("cStat", resultado.getInfEvento().getCStat());
                    responseJSON.put("xMotivo", resultado.getInfEvento().getXMotivo());
                    responseJSON.put("nProt", resultado.getInfEvento().getNProt());
                });
                
                // Cria o XML da solicitação.
                xml = CartaCorrecaoUtil.criaProcEventoCCe(config, envEvento, retorno);
                
                responseJSON.put("xml", xml);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                responseJSON.put("nProt", nProt);
                responseJSON.put("xml", xml);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }
    }
    
    /**
     * Processa o serviço NFeCancelamento.
     */
    static class NFeCancelamentoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String caminhoNFCeMonitor = System.getProperty("user.dir");
        
            String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
            String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);

            JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
            
            InputStream inputStream = httpExchange.getRequestBody(); 
            Scanner scanner = new Scanner(inputStream);
            String linha;
            String dados = new String();
            
            while (scanner.hasNextLine()) {
                linha = scanner.nextLine();
                dados = dados + linha;
            }
            
            JSONObject json = new JSONObject(dados);
            
            String cStat = "";
            String xMotivo = "";
            String nProt = "";
            String xml = "";
            
            /*
             * Prepara o XML da NFC-e.
             */
            
            // Inicia As configurações.
            ConfiguracoesNfe config;
            
            try {
                config = Config.iniciaConfiguracoes();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                responseJSON.put("nProt", nProt);
                responseJSON.put("xml", xml);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
                
                return;
            }
            
            JSONObject jsonInfEvento = json.getJSONObject("infEvento");
            
            String cnpj = jsonInfEvento.get("CNPJ").toString();
            String chNFe = jsonInfEvento.get("chNFe").toString();
            String nProtocolo = jsonInfEvento.get("nProt").toString();
            String nSeqEvento = jsonInfEvento.get("nSeqEvento").toString();
            String descEvento = jsonInfEvento.get("descEvento").toString();
            
            int sequenciaEvento = Integer.parseInt(nSeqEvento);
            
            // Cria um evento para a Carta de Correção.
            Evento cancelamento = new Evento();
            // CNPJ do emitente.
            cancelamento.setCnpj(cnpj);
            // Chave da NF-e.
            cancelamento.setChave(chNFe);
            // Protocolo de autorização da NF-e.
            cancelamento.setProtocolo(nProtocolo);
            // Data do cancelamento.
            cancelamento.setDataEvento(LocalDateTime.now());
            // Sequência do evento. Podem ser feitos até 20 cancelamentos.
            cancelamento.setSequencia(sequenciaEvento);
            // Texto do motivo do cancelamento.
            cancelamento.setMotivo(descEvento);
            
            try {
                //Monta o Evento de Cancelamento
                br.com.swconsultoria.nfe.schema.envEventoCancNFe.TEnvEvento enviEvento = CancelamentoUtil.montaCancelamento(cancelamento, config);

                //Envia o Evento de Cancelamento
                br.com.swconsultoria.nfe.schema.envEventoCancNFe.TRetEnvEvento retorno = Nfe.cancelarNfe(config, enviEvento, true, DocumentoEnum.NFCE);

                //Valida o Retorno do Cancelamento
                RetornoUtil.validaCancelamento(retorno);

                // Obtém o protocolo da Carta Correção.
                JSONObject responseJSON = new JSONObject();
                
                retorno.getRetEvento().forEach(resultado -> {
                    responseJSON.put("cStat", resultado.getInfEvento().getCStat());
                    responseJSON.put("xMotivo", resultado.getInfEvento().getXMotivo());
                    responseJSON.put("nProt", resultado.getInfEvento().getNProt());
                });
                
                // Cria o XML da solicitação.
                xml = CancelamentoUtil.criaProcEventoCancelamento(config, enviEvento, retorno.getRetEvento().get(0));
                
                responseJSON.put("xml", xml);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                responseJSON.put("nProt", nProt);
                responseJSON.put("xml", xml);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }
    }
    
    /**
     * Processa o serviço NFeInutilizacao.
     */
    static class NFeInutilizacaoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String caminhoNFCeMonitor = System.getProperty("user.dir");
        
            String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
            String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);

            JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
            
            InputStream inputStream = httpExchange.getRequestBody(); 
            Scanner scanner = new Scanner(inputStream);
            String linha;
            String dados = new String();
            
            while (scanner.hasNextLine()) {
                linha = scanner.nextLine();
                dados = dados + linha;
            }
            
            JSONObject json = new JSONObject(dados);
            
            String cStat = "";
            String xMotivo = "";
            String nProt = "";
            String xml = "";
            
            /*
             * Prepara o XML da NFC-e.
             */
            
            // Inicia As configurações.
            ConfiguracoesNfe config;
            
            try {
                config = Config.iniciaConfiguracoes();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                responseJSON.put("nProt", nProt);
                responseJSON.put("xml", xml);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
                
                return;
            }
            
            JSONObject jsonInfInut = json.getJSONObject("infInut");
            
            String cnpj = jsonInfInut.get("CNPJ").toString();
            String serie = jsonInfInut.get("serie").toString();
            String nNFIni = jsonInfInut.get("nNFIni").toString();
            String nNFFin = jsonInfInut.get("nNFFin").toString();
            String xJust = jsonInfInut.get("xJust").toString();
            
            int numeroSerie = Integer.parseInt(serie);
            int numeroInicial = Integer.parseInt(nNFIni);
            int numeroFinal = Integer.parseInt(nNFFin);
            
            LocalDateTime dataCancelamento = LocalDateTime.now();
            
            try {
                // Monta o XML de solicitação de inutilização.
                TInutNFe inutNFe = InutilizacaoUtil.montaInutilizacao(DocumentoEnum.NFCE, cnpj, numeroSerie, numeroInicial, numeroFinal, xJust, dataCancelamento, config);

                // Envia a solicitação de inutilização.
                TRetInutNFe retorno = Nfe.inutilizacao(config,inutNFe, DocumentoEnum.NFCE,true);

                // Valida o retorno.
                RetornoUtil.validaInutilizacao(retorno);
                
                // Cria o XML da solicitação.
                xml = InutilizacaoUtil.criaProcInutilizacao(config, inutNFe, retorno);
                
                cStat = retorno.getInfInut().getCStat();
                xMotivo = retorno.getInfInut().getXMotivo();
                nProt = retorno.getInfInut().getNProt();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                responseJSON.put("nProt", nProt);
                responseJSON.put("xml", xml);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                
                cStat = "000";
                xMotivo = e.getMessage();
                
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("cStat", cStat);
                responseJSON.put("xMotivo", xMotivo);
                responseJSON.put("nProt", nProt);
                responseJSON.put("xml", xml);
                
                System.out.println(responseJSON.toString());

                String response = responseJSON.toString();
                
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = httpExchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }
    }
}
