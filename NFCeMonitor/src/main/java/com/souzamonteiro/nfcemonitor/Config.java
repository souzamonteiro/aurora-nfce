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

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;
import br.com.swconsultoria.nfe.dom.enuns.EstadosEnum;
import br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.exception.NfeException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.json.JSONObject;

/**
 *
 * @author roberto
 */
public class Config {
    private static Certificado certificadoA1Pfx() throws CertificadoException {
        String caminhoNFCeMonitor = System.getProperty("user.dir");
        
        String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
        String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);
        
        JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
        
        try {
            String caminhoCertificado = configuracoes.get("caminhoCertificado").toString();
            String senhaCertificado = configuracoes.get("senhaCertificado").toString();

            return CertificadoService.certificadoPfx(caminhoCertificado, senhaCertificado);
        } catch (Exception e)  {
            System.out.println("Erro: " + e);
            
            return null;
        }
    }
    
    public static ConfiguracoesNfe iniciaConfiguracoes() throws NfeException {
        String caminhoNFCeMonitor = System.getProperty("user.dir");
        
        String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
        String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);
        
        JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
        
        try {
            String webserviceUF = configuracoes.get("webserviceUF").toString();
            String webserviceAmbiente = configuracoes.get("webserviceAmbiente").toString();
            String caminhoSchemas = configuracoes.get("caminhoSchemas").toString();
            
            Certificado certificado = certificadoA1Pfx();

            if (webserviceUF.equals("AC")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.AC , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.AC , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("AL")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.AL , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.AL , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("AM")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.AM , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.AM , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("AP")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.AP , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.AP , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("BA")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.BA , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.BA , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("CE")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.CE , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.CE , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("DF")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.DF , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.DF , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("ES")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.ES , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.ES , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("GO")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.GO , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.GO , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("MA")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.MA , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.MA , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("MG")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.MG , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.MG , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("MS")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.MS , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.MS , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("MT")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.MT , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.MT , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("PA")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.PA , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.PA , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("PB")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.PB , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.PB , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("PE")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.PE , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.PE , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("PI")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.PI , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.PI , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("PR")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.PR , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.PR , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("RJ")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RJ , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RJ , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("RN")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RN , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RN , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("RO")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RO , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RO , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("RR")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RR , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RR , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("RS")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RS , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RS , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("SC")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.SC , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.SC , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("SE")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.SE , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.SE , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("SP")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.SP , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.SP , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("TO")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.TO , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.TO , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RS , AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.RS , AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            }
        } catch (Exception e)  {
            System.out.println("Erro: " + e);
            
            return null;
        }
    }
    
    public static EstadosEnum getEstado() {
        String caminhoNFCeMonitor = System.getProperty("user.dir");
        
        String caminhoArquivoConfiguracoes = caminhoNFCeMonitor + "/NFCeMonitor.json";
        String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);
        
        JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
        
        String emitenteUF = configuracoes.get("emitenteUF").toString();
        
        if (emitenteUF.equals("AC")) {
            return EstadosEnum.AC;
        } else if (emitenteUF.equals("AL")) {
            return EstadosEnum.AL;
        } else if (emitenteUF.equals("AM")) {
            return EstadosEnum.AM;
        } else if (emitenteUF.equals("AP")) {
            return EstadosEnum.AP;
        } else if (emitenteUF.equals("BA")) {
            return EstadosEnum.BA;
        } else if (emitenteUF.equals("CE")) {
            return EstadosEnum.CE;
        } else if (emitenteUF.equals("DF")) {
            return EstadosEnum.DF;
        } else if (emitenteUF.equals("ES")) {
            return EstadosEnum.ES;
        } else if (emitenteUF.equals("GO")) {
            return EstadosEnum.GO;
        } else if (emitenteUF.equals("MA")) {
            return EstadosEnum.MA;
        } else if (emitenteUF.equals("MG")) {
            return EstadosEnum.MG;
        } else if (emitenteUF.equals("MS")) {
            return EstadosEnum.MS;
        } else if (emitenteUF.equals("MT")) {
            return EstadosEnum.MT;
        } else if (emitenteUF.equals("PA")) {
            return EstadosEnum.PA;
        } else if (emitenteUF.equals("PB")) {
            return EstadosEnum.PB;
        } else if (emitenteUF.equals("PE")) {
            return EstadosEnum.PE;
        } else if (emitenteUF.equals("PI")) {
            return EstadosEnum.PI;
        } else if (emitenteUF.equals("PR")) {
            return EstadosEnum.PR;
        } else if (emitenteUF.equals("RJ")) {
            return EstadosEnum.RJ;
        } else if (emitenteUF.equals("RN")) {
            return EstadosEnum.RN;
        } else if (emitenteUF.equals("RO")) {
            return EstadosEnum.RO;
        } else if (emitenteUF.equals("RR")) {
            return EstadosEnum.RR;
        } else if (emitenteUF.equals("RS")) {
            return EstadosEnum.RS;
        } else if (emitenteUF.equals("SC")) {
             return EstadosEnum.SC;
        } else if (emitenteUF.equals("SE")) {
            return EstadosEnum.SE;
        } else if (emitenteUF.equals("SP")) {
            return EstadosEnum.SP;
        } else if (emitenteUF.equals("TO")) {
            return EstadosEnum.TO;
        } else {
            return EstadosEnum.RS;
        }
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
            System.out.println("Erro: " + e);;
        }
        
        return data;
    }
}
