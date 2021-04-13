// Environment code for project grounded_agent

package grounded_agent;

import org.json.*;

import jason.asSyntax.*;
import jason.environment.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.*;

public class Ambiente extends Environment {

	private Timer timer;
	private int seconds = 5;
	private Logger logger = Logger.getLogger("grounded_agent." + Ambiente.class.getName());

    /** Called before the MAS execution with the args informed in .mas2j */
    @Override
    public void init(String[] args) {
        super.init(args);
        this.timer = new Timer();
        this.timer.schedule(new UpdateTask(), 0, seconds * 1000);
    }

    @Override
    public boolean executeAction(String nomeDoAgente, Structure acao) {
    	String nomeDaAcao = acao.getFunctor();
    	if (nomeDaAcao.equals("solicitarObjeto")) {
    		String objetoSolicitado = acao.getTerm(0).toString();
    		logger.info("O agente solicitou um objeto: " + objetoSolicitado);
        	return true;
        } else if (nomeDaAcao.equals("cortar")) {
        	String objetoCortavel = acao.getTerm(0).toString();
        	String objetoCortante = acao.getTerm(1).toString();
        	logger.info("O agente cortou " + objetoCortavel + " com " + objetoCortante);
        	return true;
        } else if (nomeDaAcao.equals("perguntarSeEhCortante")) {
        	String objetoQuestionado = acao.getTerm(0).toString();
        	logger.info("O agente perguntou: o objeto \"" + objetoQuestionado + "\" é cortante?");
        	responderSeEhCortante(objetoQuestionado);
        	return true;
        } else if (nomeDaAcao.equals("perguntarSeEhCortavel")) {
        	String objetoQuestionado = acao.getTerm(0).toString();
        	logger.info("O agente perguntou: o objeto \"" + objetoQuestionado + "\" é cortável?");
        	responderSeEhCortavel(objetoQuestionado);
        	return true;
        } else if (nomeDaAcao.equals("solicitarObjetoCortante")) {
        	String objCortavel = acao.getTerm(0).toString();
        	String objCortante = acao.getTerm(1).toString();
        	logger.info("O agente disse: O objeto " + objCortavel + " não é cortável por " +
        				objCortante + ".\nPreciso de um objeto para cortar " + objCortavel);
        	return true;
        } else if (nomeDaAcao.equals("perguntarSeEhCortavelPor")) {
        	String objCortavel = acao.getTerm(0).toString();
        	String objCortante = acao.getTerm(1).toString();
        	logger.info("O agente perguntou: o objeto \"" + objCortavel +
        				"\" é cortável por \"" + objCortante + "\"?");
        	responderSeEhCortavelPor(objCortavel, objCortante);
        	return true;
        }
        
        logger.info("executando: " + acao + ", mas não foi implementado!");
        return true;
    }
    
    private void responderSeEhCortante(String objeto) {
    	String crenca = "naoEhCortante";
    	if (objeto.equals("tesoura") || objeto.equals("estilete")) {
    		crenca = "ehCortante";
    	}
    	String txtNovaCrenca = crenca + "(" + objeto + ")";
    	Literal novaCrenca = Literal.parseLiteral(txtNovaCrenca);
    	logger.info("Respondendo: " + txtNovaCrenca);
    	addPercept(novaCrenca);
    }
    
    private void responderSeEhCortavel(String objeto) {
    	String crenca = "naoEhCortavel";
    	if (objeto.equals("papel") || objeto.equals("cartolina")) {
    		crenca = "ehCortavel";
    	}
    	String txtNovaCrenca = crenca + "(" + objeto + ")";
    	Literal novaCrenca = Literal.parseLiteral(txtNovaCrenca);
    	logger.info("Respondendo: " + txtNovaCrenca);
    	addPercept(novaCrenca);
    }
    
    private void responderSeEhCortavelPor(String objCortavel, String objCortante) {
    	String crenca = "naoEhCortavelPor";
    	if (objCortavel.equals("papel")) {
    		if (objCortante.equals("tesoura") || objCortante.equals("estilete")) {
    			crenca = "ehCortavelPor";
    		}
    	}
    	String txtNovaCrenca = crenca + "(" + objCortavel + "," + objCortante + ")";
    	Literal novaCrenca = Literal.parseLiteral(txtNovaCrenca);
    	logger.info("Respondendo: " + txtNovaCrenca);
    	addPercept(novaCrenca);
    }

    /** Called before the end of MAS execution */
    @Override
    public void stop() {
        super.stop();
        this.timer.cancel();
    }
    
    private void removerPercepcoesDeObjetosNaoDetectados(ArrayList<Deteccao> novasDeteccoes, List<Literal> percepcoesAtuais) {
    	for (Literal percepcao : percepcoesAtuais) {
			if (percepcao.getFunctor().equals("possui")) {
				if (!verificarSeClasseExiste(novasDeteccoes, percepcao.getTerm(0).toString())) {
					removePercept(percepcao);
				}
			}
		}
    }
    
    private void adicionarPercepcoesDeNovosObjetosDetectados(ArrayList<Deteccao> novasDeteccoes, List<Literal> percepcoesAtuais) {
    	for (Deteccao deteccao : novasDeteccoes) {
			if (!verificarSeClasseExiste(percepcoesAtuais, deteccao.getClasse().getNome())) {
				Literal possui = Literal.parseLiteral("possui(" + deteccao.getClasse().getNome() + ")");
				addPercept(possui);
			}
		}
    }
    
    private boolean verificarSeClasseExiste(List<Literal> percepcoes, String nomeDaClasse) {
    	for (Literal percepcao : percepcoes) {
    		if (percepcao.getFunctor().equals("possui")) {
    			if (percepcao.getTerm(0).toString().equals(nomeDaClasse)) {
    				return true;
    			}
    		}
		}
    	return false;
    }
    
    private boolean verificarSeClasseExiste(ArrayList<Deteccao> deteccoes, String nomeDaClasse) {
    	for (Deteccao deteccao : deteccoes) {
			if (deteccao.getClasse().getNome().equals(nomeDaClasse)) {
				return true;
			}
		}
    	return false;
    }
    
    private void atualizarPercepcoes() {
    	ArrayList<Deteccao> novasDeteccoes = lerDeteccoes();
    	List<Literal> percepcoesAtuais = consultPercepts("agent1");
    	removerPercepcoesDeObjetosNaoDetectados(novasDeteccoes, percepcoesAtuais);
    	adicionarPercepcoesDeNovosObjetosDetectados(novasDeteccoes, percepcoesAtuais);
    }
    
    private ArrayList<Deteccao> lerDeteccoes() {
    	try {
    		ArrayList<Deteccao> deteccoes = new ArrayList<Deteccao>();
    		Path filePath = new File("C:\\UFSC\\TCC\\detection_results\\agent.perception.json").toPath();
    		String json = new String(Files.readAllBytes(filePath));
			JSONArray jsonDeteccoes = new JSONArray(json);
			if (jsonDeteccoes.length() > 0) {
				for (int i = 0; i < jsonDeteccoes.length(); i++) {
					JSONObject jsonObjeto = jsonDeteccoes.getJSONObject(i);
					JSONObject jsonClasseDoObjeto = jsonObjeto.getJSONObject("class");
					int id = jsonClasseDoObjeto.getInt("id");
					String name = jsonClasseDoObjeto.getString("name");
					ClasseDeObjeto classe = new ClasseDeObjeto(id, name);
					Deteccao deteccao = new Deteccao(jsonObjeto.getFloat("score"), classe);
					if (deteccao.getScore() >= 0.5) {
						deteccoes.add(deteccao);
					}
				}
			}
			return deteccoes;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
    }
    
    private class UpdateTask extends TimerTask {
		@Override
		public void run() {
			atualizarPercepcoes();
		}
    }
    
}
