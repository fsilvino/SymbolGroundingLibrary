package grounded_agent;

public class Deteccao {

	private float score;
	private ClasseDeObjeto classe;
	
	public Deteccao(float score, ClasseDeObjeto classe) {
		this.score = score;
		this.classe = classe;
	}
	
	public float getScore() {
		return this.score;
	}
	
	public ClasseDeObjeto getClasse() {
		return this.classe;
	}
	
}
