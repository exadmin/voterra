package com.github.exadmin.voterra;

import com.github.exadmin.voterra.model.Question;
import com.github.exadmin.voterra.service.QuestionLoader;
import com.github.exadmin.voterra.service.ResultStore;
import com.github.exadmin.voterra.service.VotingService;
import com.github.exadmin.voterra.web.VoterraServer;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.parse(args);
        List<Question> questions = new QuestionLoader().load(config.questionsJsonFile());
        VotingService votingService = new VotingService(Clock.systemUTC());
        ResultStore resultStore = new ResultStore(config.resultsJsonFile());
        VoterraServer server = new VoterraServer(config, questions, votingService, resultStore);
        server.start();
        LOGGER.info("Voterra server started on port {}", config.port());
    }
}
