package br.com.justo;

import static org.junit.jupiter.api.Assertions.*;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testes de integração para operações MongoDB no contexto de pagamentos.
 *
 * <p>Princípios aplicados:
 * <ul>
 *   <li>MongoDB embarcado via Flapdoodle — sem dependência de infraestrutura real</li>
 *   <li>Isolamento total: @AfterEach dropa a coleção garantindo estado limpo</li>
 *   <li>WriteConcern.MAJORITY — durabilidade de escritas financeiras garantida</li>
 *   <li>try-with-resources em todos os cursores — zero resource leaks</li>
 *   <li>Assertions explícitas em cada operação — zero testes sem verificação</li>
 *   <li>SLF4J para logging estruturado — nunca System.out.println</li>
 *   <li>Credenciais via variáveis de ambiente — nunca hardcoded no código</li>
 * </ul>
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class TestMongo {

    private static final Logger log = LoggerFactory.getLogger(TestMongo.class);

    // =========================================================================
    // Constantes de configuração — externalizáveis via variáveis de ambiente
    // Em produção: exportar MONGO_DB e MONGO_COLLECTION antes de iniciar a JVM
    // =========================================================================
    private static final int    MONGO_PORT      = 27888;
    private static final String DB_NAME         = System.getenv().getOrDefault("MONGO_DB",        "paymentDB");
    private static final String COLLECTION_NAME = System.getenv().getOrDefault("MONGO_COLLECTION", "dados");

    // =========================================================================
    // Estado estático: iniciado uma única vez para toda a suite (@BeforeAll)
    // =========================================================================
    private static MongodExecutable   mongodExecutable;
    private static MongoClient        mongoClient;

    /** Referências por instância de teste — recriadas a cada @BeforeEach */
    private MongoDatabase             database;
    private MongoCollection<Document> collection;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    /**
     * Inicia o MongoDB embarcado UMA VEZ para toda a suite de testes.
     * Não requer MongoDB instalado ou rodando na máquina — Flapdoodle faz o download
     * do binário e inicia um processo isolado na porta {@value MONGO_PORT}.
     */
    @BeforeAll
    static void startEmbeddedMongo() throws Exception {
        log.info("Iniciando MongoDB embarcado na porta {}", MONGO_PORT);

        MongodConfig config = MongodConfig.builder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
                .build();

        mongodExecutable = MongodStarter.getDefaultInstance().prepare(config);
        mongodExecutable.start();
        mongoClient = MongoClients.create("mongodb://localhost:" + MONGO_PORT);

        log.info("MongoDB embarcado iniciado com sucesso");
    }

    /** Para o processo MongoDB embarcado após todos os testes. */
    @AfterAll
    static void stopEmbeddedMongo() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
        log.info("MongoDB embarcado encerrado");
    }

    /**
     * Obtém referências de database e collection com WriteConcern.MAJORITY.
     * Garante que toda escrita seja confirmada antes de prosseguir — obrigatório
     * para dados financeiros onde perda de dados é inaceitável.
     */
    @BeforeEach
    void setUp() {
        database   = mongoClient.getDatabase(DB_NAME).withWriteConcern(WriteConcern.MAJORITY);
        collection = database.getCollection(COLLECTION_NAME);
        log.debug("Setup: database='{}', collection='{}'", DB_NAME, COLLECTION_NAME);
    }

    /**
     * Dropa a coleção após cada teste garantindo isolamento total.
     * Sem este teardown, documentos de um teste contaminariam os seguintes,
     * tornando o comportamento de countDocuments() e queries não-determinístico.
     */
    @AfterEach
    void tearDown() {
        collection.drop();
        log.debug("Teardown: coleção '{}' removida para isolamento", COLLECTION_NAME);
    }

    // =========================================================================
    // BANCO DE DADOS E COLEÇÃO
    // =========================================================================

    @Test
    @DisplayName("[DB] Nome do banco de dados deve ser 'paymentDB'")
    void database_deveChamarSePaymentDB() {
        assertEquals(DB_NAME, database.getName(),
                "O banco de dados alvo deve ser '" + DB_NAME + "'");
    }

    @Test
    @DisplayName("[DB] Nome da coleção deve ser 'dados'")
    void collection_deveChamarSeDados() {
        assertEquals(COLLECTION_NAME, collection.getNamespace().getCollectionName(),
                "A coleção alvo deve ser '" + COLLECTION_NAME + "'");
    }

    @Test
    @DisplayName("[DB] Lista de coleções deve conter 'dados' após primeira inserção")
    void colecoes_devemConterDadosAposInsercao() {
        collection.insertOne(buildDefaultDocument());

        MongoIterable<String> names = database.listCollectionNames();
        assertNotNull(names, "listCollectionNames() não pode retornar null");

        boolean contemColecao = false;
        for (String name : names) {
            if (COLLECTION_NAME.equals(name)) {
                contemColecao = true;
                break;
            }
        }
        assertTrue(contemColecao,
                "Banco '" + DB_NAME + "' deve conter a coleção '" + COLLECTION_NAME + "'");
    }

    // =========================================================================
    // INSERÇÃO
    // =========================================================================

    @Test
    @DisplayName("[INSERT] Deve persistir campo 'name' = 'MongoDB'")
    void insert_devePersistirCampoName() {
        collection.insertOne(buildDefaultDocument());

        Document found = collection.find().first();

        assertNotNull(found, "findFirst não deve retornar null após inserção");
        assertEquals("MongoDB", found.getString("name"),
                "Campo 'name' deve ser 'MongoDB'");
    }

    @Test
    @DisplayName("[INSERT] Deve persistir campo 'type' = 'database'")
    void insert_devePersistirCampoType() {
        collection.insertOne(buildDefaultDocument());

        Document found = collection.find().first();

        assertNotNull(found);
        assertEquals("database", found.getString("type"),
                "Campo 'type' deve ser 'database'");
    }

    @Test
    @DisplayName("[INSERT] Deve persistir campo 'count' = 1")
    void insert_devePersistirCampoCount() {
        collection.insertOne(buildDefaultDocument());

        Document found = collection.find().first();

        assertNotNull(found);
        assertEquals(1, found.getInteger("count"),
                "Campo 'count' deve ser 1");
    }

    @Test
    @DisplayName("[INSERT] Deve persistir sub-documento 'info' com coordenadas x=203, y=102")
    void insert_devePersistirSubDocumentoInfo() {
        collection.insertOne(buildDefaultDocument());

        Document found = collection.find().first();
        assertNotNull(found);

        Object infoObj = found.get("info");
        assertNotNull(infoObj, "Sub-documento 'info' não pode ser null");
        assertInstanceOf(Document.class, infoObj, "Campo 'info' deve ser um Document");

        Document info = (Document) infoObj;
        assertAll("Coordenadas do sub-documento 'info'",
                () -> assertEquals(203, info.getInteger("x"), "Coordenada 'x' deve ser 203"),
                () -> assertEquals(102, info.getInteger("y"), "Coordenada 'y' deve ser 102")
        );
    }

    @Test
    @DisplayName("[INSERT] Todos os campos do documento devem estar corretos simultaneamente")
    void insert_deveRetornarDocumentoCompletoNoFind() {
        collection.insertOne(buildDefaultDocument());

        Document found = collection.find().first();
        assertNotNull(found, "Documento não pode ser null");

        assertAll("Integridade completa do documento inserido",
                () -> assertEquals("MongoDB",  found.getString("name"),   "name incorreto"),
                () -> assertEquals("database", found.getString("type"),   "type incorreto"),
                () -> assertEquals(1,          found.getInteger("count"), "count incorreto"),
                () -> assertNotNull(found.get("info"),                     "info não pode ser null")
        );
    }

    @Test
    @DisplayName("[INSERT] Uma inserção deve resultar em exatamente 1 documento na coleção")
    void insert_umaInsercao_deveResultarEmExatamenteUmDocumento() {
        assertEquals(0L, collection.countDocuments(),
                "Coleção deve estar vazia antes da primeira inserção");

        collection.insertOne(buildDefaultDocument());

        assertEquals(1L, collection.countDocuments(),
                "Deve haver exatamente 1 documento após uma inserção");
    }

    @Test
    @DisplayName("[INSERT] Duas inserções devem resultar em exatamente 2 documentos")
    void insert_duasInsercoes_deveResultarEmDoisDocumentos() {
        collection.insertOne(buildDefaultDocument());
        collection.insertOne(buildDefaultDocument());

        assertEquals(2L, collection.countDocuments(),
                "Deve haver exatamente 2 documentos após duas inserções");
    }

    // =========================================================================
    // CONSULTA — findFirst
    // =========================================================================

    @Test
    @DisplayName("[FIND] findFirst deve retornar null quando coleção está vazia")
    void findFirst_deveRetornarNullQuandoColecaoVazia() {
        assertNull(collection.find().first(),
                "findFirst deve retornar null quando não há documentos");
    }

    @Test
    @DisplayName("[FIND] findFirst deve retornar documento não-null após inserção")
    void findFirst_deveRetornarDocumentoNaoNullAposInsercao() {
        collection.insertOne(buildDefaultDocument());

        assertNotNull(collection.find().first(),
                "findFirst não deve retornar null quando há documentos");
    }

    // =========================================================================
    // CONSULTA — cursor (try-with-resources — sem resource leak)
    // =========================================================================

    @Test
    @DisplayName("[CURSOR] Cursor completo deve iterar exatamente o número de documentos inseridos")
    void cursor_deveIterarTodosOsDocumentosInseridos() {
        collection.insertOne(buildDefaultDocument());
        collection.insertOne(buildDefaultDocument());

        int count = 0;
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                assertNotNull(doc.get("info"),
                        "Campo 'info' deve existir em cada documento iterado");
                count++;
            }
        }

        assertEquals(2, count, "Cursor deve iterar exatamente 2 documentos");
    }

    @Test
    @DisplayName("[CURSOR] Cursor de coleção vazia nunca deve ter hasNext() = true")
    void cursor_vazioNaoDeveInvocarHasNext() {
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            assertFalse(cursor.hasNext(),
                    "Cursor de coleção vazia nunca deve ter próximo elemento");
        }
    }

    // =========================================================================
    // CONSULTA FILTRADA
    // =========================================================================

    @Test
    @DisplayName("[FILTER] Filtro count=1 deve retornar apenas documentos com count=1")
    void filter_porCount_deveRetornarApenasDocumentosCorrespondentes() {
        // Documento que DEVE aparecer na query
        collection.insertOne(buildDefaultDocument());

        // Documento que NÃO deve aparecer
        Document docCount2 = buildDefaultDocument();
        docCount2.put("count", 2);
        collection.insertOne(docCount2);

        int resultCount = 0;
        try (MongoCursor<Document> cursor = collection.find(Filters.eq("count", 1)).cursor()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                assertEquals(1, doc.getInteger("count"),
                        "Todos os documentos filtrados devem ter count=1");
                resultCount++;
            }
        }

        assertEquals(1, resultCount,
                "Filtro count=1 deve retornar exatamente 1 documento (não 2)");
    }

    @Test
    @DisplayName("[FILTER] Filtro count=1 não deve retornar documentos com count=2")
    void filter_porCount_naoDeveRetornarDocumentosForaDoFiltro() {
        Document docCount2 = buildDefaultDocument();
        docCount2.put("count", 2);
        collection.insertOne(docCount2);

        try (MongoCursor<Document> cursor = collection.find(Filters.eq("count", 1)).cursor()) {
            assertFalse(cursor.hasNext(),
                    "Filtro count=1 não deve retornar documentos com count=2");
        }
    }

    @Test
    @DisplayName("[FILTER] Filtro sem correspondência deve retornar cursor vazio e count=0")
    void filter_semCorrespondencia_deveRetornarCursorVazioECountZero() {
        collection.insertOne(buildDefaultDocument());

        try (MongoCursor<Document> cursor = collection.find(Filters.eq("count", 999)).cursor()) {
            assertFalse(cursor.hasNext(),
                    "Filtro sem correspondência deve retornar cursor vazio");
        }

        assertEquals(0L, collection.countDocuments(Filters.eq("count", 999)),
                "countDocuments com filtro sem correspondência deve ser 0");
    }

    @Test
    @DisplayName("[FILTER] Filtro por name deve isolar documentos corretamente")
    void filter_porName_deveIsolarDocumentosCorretos() {
        collection.insertOne(buildDefaultDocument()); // name=MongoDB

        Document outroDoc = buildDefaultDocument();
        outroDoc.put("name", "PostgreSQL");
        collection.insertOne(outroDoc);

        assertEquals(2L, collection.countDocuments(),
                "Total geral deve ser 2");
        assertEquals(1L, collection.countDocuments(Filters.eq("name", "MongoDB")),
                "Deve haver exatamente 1 documento com name='MongoDB'");
        assertEquals(1L, collection.countDocuments(Filters.eq("name", "PostgreSQL")),
                "Deve haver exatamente 1 documento com name='PostgreSQL'");
    }

    // =========================================================================
    // CONTAGEM
    // =========================================================================

    @Test
    @DisplayName("[COUNT] countDocuments deve refletir exatamente o número de inserções")
    void countDocuments_deveRefletirNumeroDeInsercoes() {
        assertEquals(0L, collection.countDocuments(), "Inicial: coleção vazia");

        collection.insertOne(buildDefaultDocument());
        assertEquals(1L, collection.countDocuments(), "Após 1 inserção: deve ser 1");

        collection.insertOne(buildDefaultDocument());
        assertEquals(2L, collection.countDocuments(), "Após 2 inserções: deve ser 2");

        collection.insertOne(buildDefaultDocument());
        assertEquals(3L, collection.countDocuments(), "Após 3 inserções: deve ser 3");
    }

    // =========================================================================
    // ISOLAMENTO
    // =========================================================================

    @Test
    @DisplayName("[ISOLAMENTO] Coleção deve estar vazia no início de cada teste")
    void isolamento_colecaoDeveEstarVaziaNoInicioCadaTeste() {
        assertEquals(0L, collection.countDocuments(),
                "Coleção deve estar vazia graças ao @AfterEach com collection.drop()");
    }

    // =========================================================================
    // FACTORY METHOD
    // =========================================================================

    /**
     * Constrói um Document MongoDB padrão para uso nos testes.
     *
     * <p>Centralizar a construção aqui garante que mudanças no schema sejam
     * refletidas em todos os testes de uma só vez.
     *
     * @return Document com campos name, type, count e sub-documento info
     */
    private Document buildDefaultDocument() {
        Document info = new Document("x", 203).append("y", 102);
        return new Document("name", "MongoDB")
                .append("type", "database")
                .append("count", 1)
                .append("info", info);
    }
}
