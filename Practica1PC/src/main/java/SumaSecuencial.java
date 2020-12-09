
import java.util.Arrays;

import es.urjc.etsii.code.concurrency.SimpleSemaphore;

import static es.urjc.etsii.code.concurrency.SimpleConcurrent.*;

public class SumaSecuencial {

	public static final int N_PROCESOS = 5;
	public static final int N_DATOS = 16;

	// Arrays
	private static int[] datos = new int[N_DATOS];

	private static int[] resultados = new int[datos.length / 2];

	// semaforos

	public static SimpleSemaphore CogerDatos;

	public static SimpleSemaphore Barrera;

	// contadores

	public static volatile int posSumar = 0;
	public static volatile int terminados = 0;
	public static volatile int nivel = 1;
	// public static volatile int puntero_actual=0;

	private static void println(String message) {// metodo imprimir
		String threadName = Thread.currentThread().getName();
		System.out.println(threadName + ": " + message);
	}

	public static void nivel() {// imprimir nivel
		if(nivel<4) {
		println("Actualiza el array de datos a " + Arrays.toString(resultados));
		}
		println("Finalizado nivel " + nivel);
	}

	public static void proceso() {
		int puntero_actual;
		while (true) {
			
			// exclusion mutua
			CogerDatos.acquire();
			puntero_actual = posSumar; // puntero array
			posSumar += 2;// apunta al siguiente valor para sumar
			// System.out.println(posSumar);
			CogerDatos.release();

			if (puntero_actual < datos.length) {// hasta que no se acaben los datos en el array, sigue entrando aqui

				println("Se inicia la suma de datos[" + puntero_actual + "]=" + datos[puntero_actual] + " y datos["
						+ (puntero_actual + 1) + "]=" + datos[puntero_actual + 1]);

				resultados[(puntero_actual + 1) / 2] = suma(datos[puntero_actual], datos[puntero_actual + 1]);

				println("Se guarda la suma en resultados[" + puntero_actual / 2 + "]="
						+ resultados[puntero_actual / 2]);

			} else {// no hay datos, bloqueamos los procesos uno a uno

				// Exclusion mutua para que vayan finalizando los proceso de 1 en 1
				CogerDatos.acquire();
				terminados++;
				
				

				if (terminados < N_PROCESOS) {// si no es el ultimo, desbloqueamos el mutex y bloqueamos el proceso
							// esperando al ultimo proceso
					println("Esperando a los demás procesos. Han terminado " + terminados + " procesos");
					CogerDatos.release();
					Barrera.acquire();
				} else {
					// si hay 4 procesos ya terminados, el ultimo proceso imprime el nivel
					// y reestablece los contadores y los arrays, por ultimo desbloquea el resto de
					// procesos

					nivel();
					System.out.println("----------------------------------------------------------------------");
					datos = resultados;
					if (datos.length > 1) {
						resultados = new int[datos.length / 2];
					}
					if (datos.length == 1) { // si solo hay 1 dato, se acabo e imprimimos el resultado
						System.out.println("El resultado es: " + datos[0]);
						return;
					}

					posSumar = 0;

					terminados = 0;
					nivel++;

					for (int i = 0; i < N_PROCESOS - 1; i++) {
						Barrera.release();

					}
					CogerDatos.release();
				}
			}
		}

	}

	private static int suma(int a, int b) {
		sleepRandom(1000);
		return a + b;
	}

	private static void inicializaDatos() {
		for (int i = 0; i < N_DATOS; i++) {
			datos[i] = (int) (Math.random() * 11);
		}

		println("Los datos a sumar son: " + Arrays.toString(datos));
	}

	public static void main(String[] args) {
		inicializaDatos();
		int sum = 0;
		for (int i = 0; i < N_DATOS; i++) {
			sum = suma(sum, datos[i]);
		}
		println("Suma: " + sum);

		CogerDatos = new SimpleSemaphore(1);
		Barrera = new SimpleSemaphore(0);
		createThreads(N_PROCESOS, "proceso");
		startThreadsAndWait();
		
	}

}