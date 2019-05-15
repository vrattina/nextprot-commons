package org.nextprot.commons.statements.reader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.nextprot.commons.statements.specs.CustomStatementField;
import org.nextprot.commons.statements.Statement;
import org.nextprot.commons.statements.StatementBuilder;
import org.nextprot.commons.statements.specs.StatementField;
import org.nextprot.commons.statements.specs.StatementSpecifications;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonReader extends StatementJsonReader {

	private final ObjectMapper mapper;

	public JsonReader(String content, StatementSpecifications specifications) {

		super(content, specifications);

		SimpleModule module = new SimpleModule();
		module.addKeyDeserializer(StatementField.class, new StatementFieldDeserializer(specifications));

		mapper = new ObjectMapper();
		mapper.registerModule(module);
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	}

	@Override
	public List<Statement> readStatements() throws IOException {

		List<Statement> statements = mapper.readValue(getContent(), new TypeReference<List<Statement>>() { });
		return statements.stream()
				.map(statement -> new StatementBuilder(statement).build())
				.collect(Collectors.toList());
	}

	/**
	 * Instanciate StatementField from key string
	 */
	private static class StatementFieldDeserializer extends KeyDeserializer {

		private final StatementSpecifications specifications;

		public StatementFieldDeserializer(StatementSpecifications specifications) {
			this.specifications = specifications;
		}

		@Override
		public StatementField deserializeKey(String key, DeserializationContext ctxt) {

			if (specifications.hasField(key)) {
				return specifications.getField(key);
			}
			return new CustomStatementField(key);
		}
	}

	public static Map<String, String> readStringMap(String jsonContent) throws IOException {

		ObjectMapper mapper = new ObjectMapper();

		return mapper.readValue(jsonContent, new TypeReference<Map<String, String>>() { });
	}
}
