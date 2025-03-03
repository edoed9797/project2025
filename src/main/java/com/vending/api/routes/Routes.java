package com.vending.api.routes;

import com.vending.ServiceRegistry;
import com.vending.api.controllers.*;
import com.vending.api.middleware.*;
import com.vending.core.services.UtenteService;
import com.vending.core.services.AdminLoginService;

import static spark.Spark.*;

/**
 * Configurazione delle rotte dell'applicazione.
 * Gestisce il routing delle richieste HTTP ai rispettivi controller
 * e applica i middleware necessari per autenticazione, logging e CORS.
 * 
 * @author Edoardo Giovanni Fracchia
 */
public class Routes {
	private final IstitutoController istitutoController;
	private final MacchinaController macchinaController;
	private final BevandaController bevandaController;
	private final ManutenzioneController manutenzioneController;
	private final AuthController authController;
	private final AuthMiddleware authMiddleware;
	private final LogMiddleware logMiddleware;
	private final CORSMiddleware corsMiddleware;

	/**
	 * Costruttore che inizializza i controller e i middleware necessari.
	 *
	 * @param istitutoController     controller per la gestione degli istituti
	 * @param macchinaController     controller per la gestione delle macchine
	 * @param bevandaController      controller per la gestione delle bevande
	 * @param manutenzioneController controller per la gestione delle manutenzioni
	 */
	public Routes(	IstitutoController istitutoController,
					MacchinaController macchinaController,
					BevandaController bevandaController,
					ManutenzioneController manutenzioneController) {
		this.istitutoController = istitutoController;
		this.macchinaController = macchinaController;
		this.bevandaController = bevandaController;
		this.manutenzioneController = manutenzioneController;
		this.authController = new AuthController(
				ServiceRegistry.get(AdminLoginService.class),
				ServiceRegistry.get(UtenteService.class));
		this.authMiddleware = new AuthMiddleware();
		this.logMiddleware = new LogMiddleware();
		this.corsMiddleware = new CORSMiddleware();

		setupRoutes();
	}

	/**
	 * Configura tutte le rotte dell'applicazione.
	 * Le rotte sono organizzate per entità (istituti, macchine, bevande,
	 * manutenzioni)
	 * e protette da autenticazione. Alcune operazioni richiedono privilegi
	 * amministrativi.
	 *
	 * Pattern delle rotte:
	 * - GET /api/{entità} - recupera tutti gli elementi
	 * - GET /api/{entità}/:id - recupera un elemento specifico
	 * - POST /api/{entità} - crea un nuovo elemento (richiede admin)
	 * - PUT /api/{entità}/:id - aggiorna un elemento (richiede admin)
	 * - DELETE /api/{entità}/:id - elimina un elemento (richiede admin)
	 */
	private void setupRoutes() {
		// Configurazione CORS e logging globale
		before((request, response) -> corsMiddleware.applicaCORS(request, response));
		after((request, response) -> logMiddleware.logRequest(request, response));

		// Root path
		get("/", (req, res) -> {
			res.redirect("/index.html");
			return null;
		});

		// Rotte pubbliche
		get("/istituti", istitutoController::getAll);
		get("/istituti/:id", istitutoController::getById);
		get("/macchine", macchinaController::getAll);
		get("/macchine/:id", macchinaController::getById);
		get("/macchine/istituto/:istitutoId", macchinaController::getByIstituto);
		get("/bevande", bevandaController::getAll);
		get("/bevande/:id", bevandaController::getById);

		// API Routes
		path("/api", () -> {
			// Auth routes (pubbliche)
			path("/auth", () -> {
				post("/login", authController::login);
				post("/register", authController::registrazione);
				post("/refresh", authController::refreshToken);
				post("/verify", authController::verificaToken);
				post("/anonymous", authController::getAnonymousToken);
			});
			;

			// Protected routes
			// Applica il middleware di autenticazione a tutte le rotte protette
			before("/protected/*", authMiddleware::autenticazione);
			path("/protected", () -> {
				// Istituti
				post("/istituti", (req, res) -> {
					authMiddleware.autorizzazioneAdmin(req, res);
					return istitutoController.create(req, res);
				});
				put("/istituti/:id", (req, res) -> {
					authMiddleware.autorizzazioneAdmin(req, res);
					return istitutoController.update(req, res);
				});
				delete("/istituti/:id", (req, res) -> {
					authMiddleware.autorizzazioneAdmin(req, res);
					return istitutoController.delete(req, res);
				});

				// Macchine
				post("/macchine", (req, res) -> {
					authMiddleware.autorizzazioneAdmin(req, res);
					return macchinaController.create(req, res);
				});
				put("/macchine/:id/stato", macchinaController::updateStato);
				delete("/macchine/:id", (req, res) -> {
					authMiddleware.autorizzazioneAdmin(req, res);
					return macchinaController.delete(req, res);
				});

				// Bevande
				path("/bevande", () -> {
					post("", (req, res) -> {
						authMiddleware.autorizzazioneAdmin(req, res);
						return bevandaController.create(req, res);
					});
					put("/:id", (req, res) -> {
						authMiddleware.autorizzazioneAdmin(req, res);
						return bevandaController.update(req, res);
					});
					delete("/:id", (req, res) -> {
						authMiddleware.autorizzazioneAdmin(req, res);
						return bevandaController.delete(req, res);
					});
					put("/:id/cialde", (req, res) -> {
						authMiddleware.autorizzazioneAdmin(req, res);
						return bevandaController.aggiungiCialda(req, res);
					});
					delete("/:id/cialde/:cialdaId", (req, res) -> {
						authMiddleware.autorizzazioneAdmin(req, res);
						return bevandaController.rimuoviCialda(req, res);
					});
				});

				// Manutenzioni
				path("/manutenzioni", () -> {
					get("", manutenzioneController::getManutenzioni);
					get("/istituto/:istitutoId", manutenzioneController::getManutenzioniIstituto);
					get("/tecnico/:tecnicoId", manutenzioneController::getManutenzioniTecnico);
					post("", manutenzioneController::iniziaManutenzione);
					put("/:id/completa", manutenzioneController::completaManutenzione);
					put("/:id/fuori-servizio", manutenzioneController::setFuoriServizio);
				});
			});
		});

		// Gestione errori
		configureErrorHandling();
	}

	private void configureErrorHandling() {
		exception(Exception.class, (e, req, res) -> {
			res.status(500);
			res.type("application/json");
			res.body("{\"errore\": \"" + e.getMessage() + "\"}");
			logMiddleware.logError(req, e);
		});

		notFound((req, res) -> {
			res.type("application/json");
			res.status(404);
			return "{\"errore\": \"Risorsa non trovata\"}";
		});

		internalServerError((req, res) -> {
			res.type("application/json");
			return "{\"errore\": \"Errore interno del server\"}";
		});
	}
}