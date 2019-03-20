package org.nextprot.commons.statements;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.nextprot.commons.algo.MD5Algo;
import org.nextprot.commons.constants.QualityQualifier;
import org.nextprot.commons.statements.constants.UniqueKey;
import org.nextprot.commons.utils.StringUtils;

import static org.nextprot.commons.statements.GenericStatementField.*;

/**
 * A StatementID is computed based on the fields when build() is invoked
 */
public class StatementBuilder {

	private Map<StatementField, String> keyValues = new TreeMap<>(Comparator.comparing(StatementField::getName));

	public static StatementBuilder createNew() {
		StatementBuilder sb = new StatementBuilder();
		return sb;
	}

	// Note: Used in bed
	public static StatementBuilder createFromExistingStatement(Statement s) {
		StatementBuilder sb = new StatementBuilder();
		sb.addMap(s);
		return sb;
	}

	public StatementBuilder addField(StatementField statementField, String statementValue) {
		this.keyValues.put(statementField, statementValue);
		return this;
	}

	public StatementBuilder addSubjects(Collection<Statement> statements) {

		Set<String> sortedStatementIds = new TreeSet<>();
		Set<String> sortedAnnotationIds = new TreeSet<>();

		Iterator<Statement> statementsIt = statements.iterator();

		while (statementsIt.hasNext()) {
			Statement s = statementsIt.next();
			sortedStatementIds.add(s.getStatementId());
			if(s.getAnnotationId() != null){
				sortedAnnotationIds.add(s.getAnnotationId());
			}else {
				if(!sortedAnnotationIds.isEmpty()){
					throw new RuntimeException("Found a null annotation id when one was already set for statement id: " + s.getStatementId());
				}
			}
		}
		
		String subjectStatemendIds = StringUtils.mkString(sortedStatementIds, ",");
		String subjectAnnotationIds = StringUtils.mkString(sortedAnnotationIds, ",");

		addField(SUBJECT_STATEMENT_IDS, subjectStatemendIds);
		
		if(!subjectAnnotationIds.isEmpty()){
			addField(GenericStatementField.SUBJECT_ANNOTATION_IDS, subjectAnnotationIds);
		}

		return this;
	}

	// Note: Used in bed
	public StatementBuilder addObject(Statement statement) {
		addField(OBJECT_STATEMENT_IDS, statement.getStatementId());
		if(statement.getAnnotationId() != null){
			addField(OBJECT_ANNOTATION_IDS, statement.getAnnotationId());
			
		}
		return this;
	}

	public StatementBuilder addMap(Map<StatementField, String> map) {
		keyValues.putAll(map);
		return this;
	}

	public StatementBuilder addCompulsoryFields(String entryAccession, String isoformAccession, String annotationCategory, QualityQualifier quality) {
		addField(ENTRY_ACCESSION, entryAccession);
		addField(ANNOTATION_CATEGORY, annotationCategory);
		addQuality(quality);
		return this;
	}

	// Note: Used in bed
	public StatementBuilder addCvTerm(String cvTermAccession, String cvTermName, String cvTerminology) {
		addField(ANNOT_CV_TERM_ACCESSION, cvTermAccession);
		addField(ANNOT_CV_TERM_NAME, cvTermName);
		addField(ANNOT_CV_TERM_TERMINOLOGY, cvTerminology);
		return this;
	}

	public StatementBuilder addQuality(QualityQualifier quality) {
		addField(GenericStatementField.EVIDENCE_QUALITY, quality.name());
		return this;
	}

	
	public StatementBuilder addSourceInfo(String sourceAccession, String sourceDatabase) {
		addField(ANNOT_SOURCE_ACCESSION, sourceAccession);
		addField(SOURCE, sourceDatabase);
		return this;
	}


	public StatementBuilder addDebugInfo(String info) {
		addField(DEBUG_INFO, info);
		return this;
	}

	// Note: Used in bed
	public StatementBuilder addVariantInfo(String annotationCategory, String firstPosition, String lastPosition, String variationOrigin, String variationVariation) {

		if(annotationCategory == null || !(annotationCategory.equals("variant") || annotationCategory.equals("mutagenesis"))) {
			throw new RuntimeException("annotation category " + annotationCategory + " is not allowed for variant");
		}
		addField(ANNOTATION_CATEGORY, annotationCategory);
		
		addField(LOCATION_BEGIN, firstPosition);
		addField(LOCATION_END, lastPosition);

		addField(VARIANT_ORIGINAL_AMINO_ACID, variationOrigin);
		addField(VARIANT_VARIATION_AMINO_ACID, variationVariation);

		return this;

	}

	public Statement build() {
		Statement rs = new Statement(keyValues);
		rs.putValue(GenericStatementField.STATEMENT_ID, computeUniqueKey(rs, UniqueKey.STATEMENT));
		return rs;
	}

	public Statement buildWithAnnotationHash() {
		Statement rs = build();
		rs.putValue(GenericStatementField.ANNOTATION_ID, computeUniqueKey(rs, UniqueKey.ENTRY));
		return rs;
	}

	/**
	 * This method compute a MD5 unique key based on the combination of selected statement fields
	 *
	 * @param statement the statement to compute unique key on
	 * @param uniqueKey the type of unique key to create
	 * @return a unique key as string
	 * @implSpec at https://calipho.isb-sib.ch/wiki/display/cal/Raw+statements+specifications
	 */
	public static String computeUniqueKey(Statement statement, UniqueKey uniqueKey) {

		// Filter fields which are used to compute unicity key
		Set<StatementField> unicityFields = new HashSet<>();

		for (StatementField field : statement.keySet()) {

			// ENTRY TYPE: only fields that are part of unicity key are considered
			if (uniqueKey.equals(UniqueKey.ENTRY)) {
				if (field.isPartOfAnnotationUnicityKey()) {
					unicityFields.add(field);
				}
			}
			// STATEMENT TYPE: all fields are considered to build the unique key
			else if (uniqueKey.equals(UniqueKey.STATEMENT)) { // All fields for the statement
				if (!field.equals(GenericStatementField.STATEMENT_ID) && !field.equals(GenericStatementField.ANNOTATION_ID)) {
					unicityFields.add(field);
				}
			}
		}

		if (unicityFields.isEmpty()) {
			throw new IllegalStateException("could not compute a unique key for statement "+statement + " (type="+ uniqueKey +")");
		}

		return MD5Algo.computeMD5(unicityFields.stream()
				.map(statement::getValue)
				.collect(Collectors.joining("")));
	}
}
