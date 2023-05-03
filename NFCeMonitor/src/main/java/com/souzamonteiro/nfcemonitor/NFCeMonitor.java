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

import br.com.swconsultoria.impressao.model.Impressao;
import br.com.swconsultoria.impressao.service.ImpressaoService;
import br.com.swconsultoria.impressao.util.ImpressaoUtil;

import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.*;
import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.exception.NfeException;
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
import br.com.swconsultoria.nfe.schema_4.retConsReciNFe.TRetConsReciNFe;
import br.com.swconsultoria.nfe.util.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONArray;
import org.json.JSONObject;

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
        server.createContext("/enviar", new EnviarHandler());
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
     * Processa uma conexão HTTP.
     */
    static class EnviarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String caminhoNFCeMonitor = System.getProperty("user.dir");
        
            String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
            String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);

            JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
            
            String webserviceAmbiente = configuracoes.get("webserviceAmbiente").toString();
            String webserviceConsulta = configuracoes.get("webserviceConsulta").toString();
            String caminhoXML = configuracoes.get("caminhoXML").toString();
            
            InputStream inputStream = httpExchange.getRequestBody(); 
            Scanner scanner = new Scanner(inputStream);
            String linha;
            String dados = new String();
            
            while (scanner.hasNextLine()) {
                linha = scanner.nextLine();
                dados = dados + linha;
            }
            
            JSONObject json = new JSONObject(dados);
            System.out.println(json.toString());
            
            /*
             * Prepara o XML da NFC-e.
             */
            String status = "";
            String motivo = "";
            String protocolo = "";
            String xml = "";

            try {
                // Inicia As configurações.
                ConfiguracoesNfe config = Config.iniciaConfiguracoes();
                
                JSONObject jsonInfNFe = json.getJSONObject("infNFe");
                JSONObject jsonIde = jsonInfNFe.getJSONObject("ide");
                JSONObject jsonEmit = jsonInfNFe.getJSONObject("emit");
                JSONObject jsonEnderEmit = jsonEmit.getJSONObject("enderEmit");
                JSONObject jsonDest = jsonInfNFe.getJSONObject("dest");
                JSONObject jsonEnderDest = jsonDest.getJSONObject("enderDest");
                JSONObject jsonAutXML = jsonInfNFe.getJSONObject("autXML");
                JSONArray jsonDet = jsonInfNFe.getJSONArray("det");
                JSONObject jsonTotal = jsonInfNFe.getJSONObject("total");
                JSONObject jsonICMSTot = jsonTotal.getJSONObject("ICMSTot");
                JSONObject jsonTransp = jsonInfNFe.getJSONObject("transp");
                JSONArray jsonPag = jsonInfNFe.getJSONArray("pag");
                JSONObject jsonInfAdic = jsonInfNFe.getJSONObject("infAdic");
                
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
                // Tipo de emissao da NFC-e.
                String tipoEmissao = jsonIde.get("tpEmis").toString();
                
                // Id do token de emissão da NFC-e..
                String idToken = configuracoes.get("idToken").toString();
                // CSC da NFC-e.
                String csc = configuracoes.get("CSC").toString();

                // Chave da NFC-e.
                ChaveUtil chaveUtil = new ChaveUtil(config.getEstado(), cnpj, modelo, serie, numeroNFCe, tipoEmissao, cnf, dataEmissao);
                String chave = chaveUtil.getChaveNF();
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
                ide.setTpEmis(tipoEmissao);
                ide.setCDV(cdv);
                ide.setTpAmb(config.getAmbiente().getCodigo());
                ide.setFinNFe(jsonIde.get("finNFe").toString());
                ide.setIndFinal(jsonIde.get("indFinal").toString());
                ide.setIndPres(jsonIde.get("indPres").toString());
                ide.setProcEmi(jsonIde.get("procEmi").toString());
                ide.setVerProc(jsonIde.get("verProc").toString());

                infNFe.setIde(ide);

                // Preenche o Emitente.
                Emit emit = new Emit();
                emit.setCNPJ(cnpj);
                emit.setIE(ie);
                emit.setXNome(jsonEmit.get("xNome").toString());

                TEnderEmi enderEmit = new TEnderEmi();
                enderEmit.setXLgr(jsonEnderEmit.get("xLgr").toString());
                enderEmit.setNro(jsonEnderEmit.get("nro").toString());
                enderEmit.setXCpl(jsonEnderEmit.get("xCpl").toString());
                enderEmit.setXBairro(jsonEnderEmit.get("xBairro").toString());
                enderEmit.setCMun(jsonEnderEmit.get("cMun").toString());
                enderEmit.setXMun(jsonEnderEmit.get("xMun").toString());
                enderEmit.setUF(TUfEmi.valueOf(config.getEstado().toString()));
                enderEmit.setCEP(jsonEnderEmit.get("CEP").toString());
                enderEmit.setCPais(jsonEnderEmit.get("cPais").toString());
                enderEmit.setXPais(jsonEnderEmit.get("xPais").toString());
                enderEmit.setFone(jsonEnderEmit.get("fone").toString());
                emit.setEnderEmit(enderEmit);

                emit.setCRT(jsonEmit.get("CRT").toString());
                
                infNFe.setEmit(emit);

                // Preenche o Destinatario.
                Dest dest = new Dest();
                dest.setCPF(jsonDest.get("CPF").toString());
                
                if (webserviceAmbiente.equals("2")) {
                    dest.setXNome("NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL");
                } else {
                    dest.setXNome(jsonDest.get("xNome").toString());
                }
                TEndereco enderDest = new TEndereco();
                enderDest.setXLgr(jsonEnderDest.get("xLgr").toString());
                enderDest.setNro(jsonEnderDest.get("nro").toString());
                enderDest.setXBairro(jsonEnderDest.get("xBairro").toString());
                enderDest.setCMun(jsonEnderDest.get("cMun").toString());
                enderDest.setXMun(jsonEnderDest.get("xMun").toString());
                enderDest.setUF(TUf.valueOf(jsonEnderDest.get("UF").toString()));
                enderDest.setCEP(jsonEnderDest.get("CEP").toString());
                enderDest.setCPais(jsonEnderDest.get("cPais").toString());
                enderDest.setXPais(jsonEnderDest.get("xPais").toString());
                enderDest.setFone(jsonEnderDest.get("fone").toString());
                dest.setEnderDest(enderDest);
                dest.setIndIEDest("9");
                
                infNFe.setDest(dest);

                // Preenche os dados do contador.
                AutXML autXML = new AutXML();
                autXML.setCNPJ(jsonAutXML.get("CNPJ").toString());
                infNFe.getAutXML().add(autXML);

                // Preenche os dados do Produto da NFC-e e adiciona à lista de produtos.
                for(int i = 0; i < jsonDet.length(); i++){
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
                    JSONObject jsonPIS = jsonImposto.getJSONObject("PIS");
                    JSONObject jsonPISAliq = jsonPIS.getJSONObject("PISAliq");
                    JSONObject jsonCOFINS = jsonImposto.getJSONObject("COFINS");
                    JSONObject jsonCOFINSAliq = jsonCOFINS.getJSONObject("COFINSAliq");
                    
                    int n = i + 1;
                    
                    Det det = new Det();

                    // O numero do item.
                    det.setNItem(Integer.toString(n));

                    // Preenche os dados dos Produtos.
                    Prod prod = new Prod();
                    prod.setCProd(jsonProd.get("cProd").toString());
                    prod.setCEAN(jsonProd.get("cEAN").toString());
                    if (webserviceAmbiente.equals("2")) {
                        prod.setXProd("NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL");
                    } else {
                        prod.setXProd(jsonProd.get("xProd").toString());
                    }
                    prod.setNCM(jsonProd.get("NCM").toString());
                    prod.setCEST(jsonProd.get("CEST").toString());
                    prod.setIndEscala(jsonProd.get("indEscala").toString());
                    prod.setCFOP(jsonProd.get("CFOP").toString());
                    prod.setUCom(jsonProd.get("uCom").toString());
                    prod.setQCom(jsonProd.get("qCom").toString());
                    prod.setVUnCom(jsonProd.get("vUnCom").toString());
                    prod.setVProd(jsonProd.get("vProd").toString());
                    prod.setCEANTrib(jsonProd.get("cEANTrib").toString());
                    prod.setUTrib(jsonProd.get("uTrib").toString());
                    prod.setQTrib(jsonProd.get("qTrib").toString());
                    prod.setVUnTrib(jsonProd.get("vUnTrib").toString());
                    prod.setIndTot(jsonProd.get("indTot").toString());

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
                        icms.setICMS10(icms10);
                    }
                    if (jsonICMS.has("ICMS20")) {
                        ICMS.ICMS20 icms20 = new ICMS.ICMS20();
                        icms20.setOrig(jsonICMS20.get("orig").toString());
                        icms20.setCST(jsonICMS20.get("CST").toString());
                        icms20.setModBC(jsonICMS20.get("modBC").toString());
                        icms20.setVBC(jsonICMS20.get("vBC").toString());
                        icms20.setPICMS(jsonICMS20.get("pICMS").toString());
                        icms20.setVICMS(jsonICMS20.get("vICMS").toString());
                        icms.setICMS20(icms20);
                    }
                    if (jsonICMS.has("ICMS30")) {
                        ICMS.ICMS30 icms30 = new ICMS.ICMS30();
                        icms30.setOrig(jsonICMS30.get("orig").toString());
                        icms30.setCST(jsonICMS30.get("CST").toString());
                        icms30.setModBCST(jsonICMS30.get("modBCST").toString());
                        icms30.setPMVAST(jsonICMS30.get("pMVAST").toString());
                        icms30.setPRedBCST(jsonICMS30.get("pRedBCST").toString());
                        icms30.setVBCST(jsonICMS30.get("vBCST").toString());
                        icms30.setPICMSST(jsonICMS30.get("pICMSST").toString());
                        icms30.setVICMSST(jsonICMS30.get("vICMSST").toString());
                        icms.setICMS30(icms30);
                    }
                    if (jsonICMS.has("ICMS40")) {
                        ICMS.ICMS40 icms40 = new ICMS.ICMS40();
                        icms40.setOrig(jsonICMS40.get("orig").toString());
                        icms40.setCST(jsonICMS40.get("CST").toString());
                        icms40.setVICMSDeson(jsonICMS40.get("vICMSDeson").toString());
                        icms.setICMS40(icms40);
                    }
                    if (jsonICMS.has("ICMS51")) {
                        ICMS.ICMS51 icms51 = new ICMS.ICMS51();
                        icms51.setOrig(jsonICMS51.get("orig").toString());
                        icms51.setCST(jsonICMS51.get("CST").toString());
                        icms51.setModBC(jsonICMS51.get("modBC").toString());
                        icms51.setVBC(jsonICMS51.get("vBC").toString());
                        icms51.setPICMS(jsonICMS51.get("pICMS").toString());
                        icms51.setVICMS(jsonICMS51.get("vICMS").toString());
                        icms.setICMS51(icms51);
                    }
                    if (jsonICMS.has("ICMS60")) {
                        ICMS.ICMS60 icms60 = new ICMS.ICMS60();
                        icms60.setOrig(jsonICMS60.get("orig").toString());
                        icms60.setCST(jsonICMS60.get("CST").toString());
                        icms60.setVBCSTRet(jsonICMS60.get("vBCSTRet").toString());
                        icms60.setPST(jsonICMS60.get("pST").toString());
                        icms60.setVICMSSubstituto(jsonICMS60.get("vICMSSubstituto").toString());
                        icms60.setVICMSSTRet(jsonICMS60.get("vICMSSTRet").toString());
                        icms60.setVBCFCPSTRet(jsonICMS60.get("vBCFCPSTRet").toString());
                        icms60.setPFCPSTRet(jsonICMS60.get("pFCPSTRet").toString());
                        icms60.setPRedBCEfet(jsonICMS60.get("pRedBCEfet").toString());
                        icms60.setVBCEfet(jsonICMS60.get("vBCEfet").toString());
                        icms60.setPICMSEfet(jsonICMS60.get("pICMSEfet").toString());
                        icms60.setVICMSEfet(jsonICMS60.get("vICMSEfet").toString());
                        icms.setICMS60(icms60);
                    }
                    if (jsonICMS.has("ICMS70")) {
                        ICMS.ICMS70 icms70 = new ICMS.ICMS70();
                        icms70.setOrig(jsonICMS70.get("orig").toString());
                        icms70.setCST(jsonICMS70.get("CST").toString());
                        icms70.setModBC(jsonICMS70.get("modBC").toString());
                        icms70.setVBC(jsonICMS70.get("vBC").toString());
                        icms70.setPICMS(jsonICMS70.get("pICMS").toString());
                        icms70.setVICMS(jsonICMS70.get("vICMS").toString());
                        icms.setICMS70(icms70);
                    }
                    if (jsonICMS.has("ICMS90")) {
                        ICMS.ICMS90 icms90 = new ICMS.ICMS90();
                        icms90.setOrig(jsonICMS90.get("orig").toString());
                        icms90.setCST(jsonICMS90.get("CST").toString());
                        icms90.setModBC(jsonICMS90.get("modBC").toString());
                        icms90.setVBC(jsonICMS90.get("vBC").toString());
                        icms90.setPICMS(jsonICMS90.get("pICMS").toString());
                        icms90.setVICMS(jsonICMS90.get("vICMS").toString());
                        icms.setICMS90(icms90);
                    }
                    
                    PIS pis = new PIS();
                    PISAliq pisAliq = new PISAliq();
                    pisAliq.setCST(jsonPISAliq.get("CST").toString());
                    pisAliq.setVBC(jsonPISAliq.get("vBC").toString());
                    pisAliq.setPPIS(jsonPISAliq.get("pPIS").toString());
                    pisAliq.setVPIS(jsonPISAliq.get("vPIS").toString());
                    pis.setPISAliq(pisAliq);

                    COFINS cofins = new COFINS();
                    COFINSAliq cofinsAliq = new COFINSAliq();
                    cofinsAliq.setCST(jsonCOFINSAliq.get("CST").toString());
                    cofinsAliq.setVBC(jsonCOFINSAliq.get("vBC").toString());
                    cofinsAliq.setPCOFINS(jsonCOFINSAliq.get("pCOFINS").toString());
                    cofinsAliq.setVCOFINS(jsonCOFINSAliq.get("vCOFINS").toString());
                    cofins.setCOFINSAliq(cofinsAliq);

                    JAXBElement<ICMS> icmsElement = new JAXBElement<ICMS>(new QName("ICMS"), ICMS.class, icms);
                    imposto.getContent().add(icmsElement);

                    JAXBElement<PIS> pisElement = new JAXBElement<PIS>(new QName("PIS"), PIS.class, pis);
                    imposto.getContent().add(pisElement);

                    JAXBElement<COFINS> cofinsElement = new JAXBElement<COFINS>(new QName("COFINS"), COFINS.class, cofins);
                    imposto.getContent().add(cofinsElement);

                    det.setImposto(imposto);

                    infNFe.getDet().addAll(Collections.singletonList(det));
                }
                
                // Preenche os totais da NFC-e.
                Total total = new Total();
                ICMSTot icmstot = new ICMSTot();
                icmstot.setVBC(jsonICMSTot.get("vBC").toString());
                icmstot.setVICMS(jsonICMSTot.get("vICMS").toString());
                icmstot.setVICMSDeson(jsonICMSTot.get("vICMSDeson").toString());
                icmstot.setVFCP(jsonICMSTot.get("vFCP").toString());
                icmstot.setVFCPST(jsonICMSTot.get("vBCST").toString());
                icmstot.setVFCPSTRet(jsonICMSTot.get("vST").toString());
                icmstot.setVBCST(jsonICMSTot.get("vFCPST").toString());
                icmstot.setVST(jsonICMSTot.get("vFCPSTRet").toString());
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
                total.setICMSTot(icmstot);

                infNFe.setTotal(total);

                // Preenche os dados do Transporte.
                Transp transp = new Transp();
                transp.setModFrete(jsonTransp.get("modFrete").toString());

                infNFe.setTransp(transp);

                // Preenche dados dos Pagamentos.
                Pag pag = new Pag();
                
                for(int i = 0; i < jsonPag.length(); i++){
                    JSONObject jsonDetPag = jsonPag.getJSONObject(i);
                    Pag.DetPag detPag = new Pag.DetPag();
                    if (jsonDetPag.has("indPag")) {
                        detPag.setIndPag(jsonDetPag.get("indPag").toString());
                    }
                    detPag.setTPag(jsonDetPag.get("tPag").toString());
                    detPag.setVPag(jsonDetPag.get("vPag").toString());
                    if (jsonDetPag.has("card")) {
                        JSONObject jsonCard = jsonDetPag.getJSONObject("card");
                        
                        Pag.DetPag.Card card = new Pag.DetPag.Card();
                        card.setTpIntegra(jsonCard.get("tpIntegra").toString());
                        card.setCNPJ(jsonCard.get("CNPJ").toString());
                        card.setTBand(jsonCard.get("tBand").toString());
                        card.setCAut(jsonCard.get("cAut").toString());
                        detPag.setCard(card);
                    }
                    pag.getDetPag().add(detPag);
                }
                
                infNFe.setPag(pag);
                
                InfAdic infAdic = new InfAdic();
                infAdic.setInfCpl(jsonInfAdic.get("infCpl").toString());
                infNFe.setInfAdic(infAdic);
                        
                TNFe nfe = new TNFe();
                nfe.setInfNFe(infNFe);

                // Monta a EnviNfe.
                TEnviNFe enviNFe = new TEnviNFe();
                enviNFe.setVersao(ConstantesUtil.VERSAO.NFE);
                enviNFe.setIdLote("1");
                enviNFe.setIndSinc("1");
                enviNFe.getNFe().add(nfe);

                // Monta e Assina o XML.
                enviNFe = Nfe.montaNfe(config, enviNFe, true);

                // Monta o QR Code.
                String qrCode;
                if (tipoEmissao.equals("9")) {
                    qrCode = preencheQRCodeContingencia(enviNFe, config, idToken, csc);
                } else {
                    qrCode = preencheQRCode(enviNFe, config, idToken, csc);
                }
                
                TNFe.InfNFeSupl infNFeSupl = new TNFe.InfNFeSupl();
                infNFeSupl.setQrCode(qrCode);
                infNFeSupl.setUrlChave(WebServiceUtil.getUrl(config, DocumentoEnum.NFCE, ServicosEnum.URL_CONSULTANFCE));
                enviNFe.getNFe().get(0).setInfNFeSupl(infNFeSupl);

                // Envia a NFC-e para a SEFAZ.
                TRetEnviNFe retorno = Nfe.enviarNfe(config, enviNFe, DocumentoEnum.NFCE);
                
                // VErifica se o retorno é assíncrono.
                if (RetornoUtil.isRetornoAssincrono(retorno)) {
                    // Obtém o Recibo.
                    String recibo = retorno.getInfRec().getNRec();
                    int tentativa = 0;
                    TRetConsReciNFe retornoNfe = null;

                    // Realiza a consulta diversas vezes.
                    while (tentativa < 15) {
                        retornoNfe = Nfe.consultaRecibo(config, recibo, DocumentoEnum.NFE);
                        if (retornoNfe.getCStat().equals(StatusEnum.LOTE_EM_PROCESSAMENTO.getCodigo())) {
                            System.out.println("INFO: Lote Em Processamento, vai tentar novamente apos 1 Segundo.");
                            Thread.sleep(1000);
                            tentativa++;
                        } else {
                            break;
                        }
                    }

                    RetornoUtil.validaAssincrono(retornoNfe);
                    
                    status = retornoNfe.getProtNFe().get(0).getInfProt().getCStat();
                    motivo = retornoNfe.getProtNFe().get(0).getInfProt().getXMotivo();
                    
                    System.out.println("Status: " + status + " - " + motivo);
                    if (status.equals("100")) {
                        protocolo = retornoNfe.getProtNFe().get(0).getInfProt().getNProt();
                        xml = XmlNfeUtil.criaNfeProc(enviNFe, retornoNfe.getProtNFe().get(0));
                        
                        System.out.println("Protocolo: " + protocolo);
                        System.out.println("XML Final: " + xml);
                    }
                } else {
                    // Retorno síncrono.
                    RetornoUtil.validaSincrono(retorno);
                    
                    status = retorno.getProtNFe().getInfProt().getCStat();
                    motivo = retorno.getProtNFe().getInfProt().getXMotivo();
                    
                    System.out.println("Status: " + status + " - " + motivo);
                    if (status.equals("100")) {
                        protocolo = retorno.getProtNFe().getInfProt().getNProt();
                        xml = XmlNfeUtil.criaNfeProc(enviNFe, retorno.getProtNFe());
                        
                        // Salva o XML da NFC-e.
                        FileWriter writer = new FileWriter(caminhoXML + "/NFe" + chave + ".xml");
                        writer.write(xml);
                        writer.close();      
                       
                        // Utiliza layout de impressão padrão.
                        Impressao impressao = ImpressaoUtil.impressaoPadraoNFCe(xml, webserviceConsulta);

                        //Faz a impressão em pdf File passando o caminho do Arquivo
                        ImpressaoService.impressaoPdfArquivo(impressao, caminhoXML + "/NFe" + chave + ".pdf");

                        System.out.println("Protocolo: " + protocolo);
                        System.out.println("XML Final: " + xml);
                    }
                }
            } catch (Exception e) {
                status = "000";
                motivo = e.getMessage();
                
                System.err.println("Erro: " + e.getMessage());
            }
            
            JSONObject responseJSON = new JSONObject();
            responseJSON.put("status", status);
            responseJSON.put("motivo", motivo);
            responseJSON.put("protocolo", protocolo);
            responseJSON.put("xml", xml);
            
            System.out.println(responseJSON.toString());
            
            String response = responseJSON.toString();
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }
    
    private static String preencheQRCode(TEnviNFe enviNFe, ConfiguracoesNfe config, String idToken, String csc) throws NfeException, NoSuchAlgorithmException {
        return NFCeUtil.getCodeQRCode(
            enviNFe.getNFe().get(0).getInfNFe().getId().substring(3),
            config.getAmbiente().getCodigo(),
            idToken,
            csc,
            WebServiceUtil.getUrl(config,DocumentoEnum.NFCE, ServicosEnum.URL_QRCODE));
    }
    
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
}
