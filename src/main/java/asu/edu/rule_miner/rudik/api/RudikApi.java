package asu.edu.rule_miner.rudik.api;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import asu.edu.rule_miner.rudik.api.model.HornRuleResult;
import asu.edu.rule_miner.rudik.api.model.HornRuleResult.RuleType;
import asu.edu.rule_miner.rudik.api.model.RudikResult;
import asu.edu.rule_miner.rudik.configuration.ConfigurationFacility;
import asu.edu.rule_miner.rudik.model.horn_rule.HornRule;
import asu.edu.rule_miner.rudik.model.rdf.graph.Graph;
import asu.edu.rule_miner.rudik.predicate.analysis.KBPredicateSelector;
import asu.edu.rule_miner.rudik.predicate.analysis.SparqlKBPredicateSelector;
import asu.edu.rule_miner.rudik.rule_generator.DynamicPruningRuleDiscovery;
import asu.edu.rule_miner.rudik.rule_generator.HornRuleDiscoveryInterface;

public class RudikApi {

  private HornRuleDiscoveryInterface ruleDiscovery;
  private KBPredicateSelector kbAnalysis;
  private SorroundingGraphGeneration graphGeneration;
  private RuleInstanceGeneration ruleInstantiation;

  private final String inputExampleFiles = "src/main/resources/input_examples/";

  // by default, timeout set to 10 minutes
  private final int timeout = 10 * 60;

  private int maxInstantiationNumber = 1000;

  /**
   * Initialize RudikApi with a specific configuration file that contains all parameters used by RuDiK
   *
   * @param filePath
   * @param timeout - timeout, in seconds, to specify the max waiting time for each operation. If an operation takes longer
   * than the timeout, then the operation is killed and it returns an empty result
   */
  public RudikApi(final String filePath, int timeout) {
    ConfigurationFacility.setConfigurationFile(filePath);
    initialiseObjects(timeout);
  }

  public RudikApi() {
    initialiseObjects(timeout);
  }

  private void initialiseObjects(int timeout) {
    this.ruleDiscovery = new DynamicPruningRuleDiscovery();
    this.kbAnalysis = new SparqlKBPredicateSelector();
    this.graphGeneration = new SorroundingGraphGeneration(ruleDiscovery.getSparqlExecutor(), timeout);
    this.ruleInstantiation = new RuleInstanceGeneration(ruleDiscovery, timeout);
  }

  /**
   * Compute positive rules for the target predicate and return a RudikResult
   *
   * @param targetPredicate
   * @return
   */
  public RudikResult discoverPositiveRules(final String targetPredicate) {
    final boolean isInstantiation=false;
    return discoverRules(targetPredicate, RuleType.positive, isInstantiation);
  }

  /**
   * Compute negative rules for the target predicate and return a RudikResult
   *
   * @param targetPredicate
   * @return
   */
  public RudikResult discoverNegativeRules(final String targetPredicate) {
    final boolean isInstantiation=false;
    return discoverRules(targetPredicate, RuleType.negative,isInstantiation);
  }

  private RudikResult discoverRules(final String targetPredicate, RuleType type,boolean isInstantiation) {
    final Pair<String, String> subjectObjectType = kbAnalysis.getPredicateTypes(targetPredicate);
    final String typeSubject = subjectObjectType.getLeft();
    final String typeObject = subjectObjectType.getRight();
    final Set<String> relations = Sets.newHashSet(targetPredicate);

    // check positive/negative examples are provided in the input file
    String suffix = type == RuleType.positive ? "_generation" : "_validation";
    final String predicateFile = targetPredicate.replaceAll("/", "_").replaceAll(":", "_").replaceAll("\\.", "_");
    final File posExFile = new File(inputExampleFiles + predicateFile + "_" + type.toString() + suffix + ".txt");
    Set<Pair<String, String>> positiveExamples = null;
    if (posExFile.exists()) {
      positiveExamples = readExamples(posExFile);
    } else {
      positiveExamples = ruleDiscovery.generatePositiveExamples(relations, typeSubject, typeObject);
    }

    suffix = type == RuleType.positive ? "_validation" : "_generation";
    Set<Pair<String, String>> negativeExamples = null;
    final File negExFile = new File(inputExampleFiles + predicateFile + "_" + type.toString() + suffix + ".txt");
    if (negExFile.exists()) {
      negativeExamples = readExamples(negExFile);
    } else {
      negativeExamples = ruleDiscovery.generateNegativeExamples(relations, typeSubject, typeObject);
    }

    writeExamplesToFile(positiveExamples, negativeExamples, predicateFile, type);

    Map<HornRule, Double> outputRules = null;
    if (type == RuleType.positive) {
      outputRules = ruleDiscovery.discoverPositiveHornRules(negativeExamples, positiveExamples, relations, typeSubject,
          typeObject);
    } else {
      outputRules = ruleDiscovery.discoverNegativeHornRules(negativeExamples, positiveExamples, relations, typeSubject,
          typeObject);
    }
    return buildResult(outputRules.keySet(), targetPredicate, type, typeSubject, typeObject, isInstantiation);
  }

  private void writeExamplesToFile(Set<Pair<String, String>> positiveExamples,
      Set<Pair<String, String>> negativeExamples, String predicateFile, RuleType type) {
    try {
      String suffix = type == RuleType.positive ? "_generation" : "_validation";
      final BufferedWriter wr = new BufferedWriter(
          new FileWriter(new File(predicateFile + "_" + type.toString() + suffix + ".txt")));
      positiveExamples.forEach(p -> {
        try {
          wr.write(p.getLeft() + "\t" + p.getRight() + "\n");
        } catch (final IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      });
      wr.close();
      suffix = type == RuleType.positive ? "_validation" : "_generation";
      final BufferedWriter newWr = new BufferedWriter(
          new FileWriter(new File(predicateFile + "_" + type.toString() + suffix + ".txt")));
      negativeExamples.forEach(n -> {
        try {
          newWr.write(n.getLeft() + "\t" + n.getRight() + "\n");
        } catch (final IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      });
      newWr.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private Set<Pair<String, String>> readExamples(File f) {
    final Set<Pair<String, String>> exs = Sets.newHashSet();
    try {
      final BufferedReader reader = new BufferedReader(new FileReader(f));
      String line;
      while ((line = reader.readLine()) != null) {
        final String[] lineSplit = line.split("\t");
        exs.add(Pair.of(lineSplit[0], lineSplit[1]));
      }
      reader.close();
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return exs;
  }

  /**
   * Instantiate a HornRule over the KB and return a Rudik result containing all the information of the rules instantiated
   *
   * @param rule
   * @param targetPredicate
   * @param type
   * @return
   */
  public RudikResult instantiateSingleRule(HornRule rule, String targetPredicate, RuleType type) {
    //to build the instantiations and surrounding graph in buikdIndividualresult()
    final boolean isInstantiation=true;
    final Pair<String, String> subjectObjectType = kbAnalysis.getPredicateTypes(targetPredicate);
    final String typeSubject = subjectObjectType.getLeft();
    final String typeObject = subjectObjectType.getRight();
    return buildResult(Lists.newArrayList(rule), targetPredicate, type, typeSubject, typeObject,isInstantiation);
  }

  private RudikResult buildResult(Collection<HornRule> allRules, String targetPredicate, RuleType type, String subType,
      String objType, boolean isInstantiation) {
    final RudikResult result = new RudikResult();
    allRules.forEach(rule -> result.addResult(buildIndividualResult(rule, targetPredicate, type, subType, objType,isInstantiation)));
    return result;
  }

  private HornRuleResult buildIndividualResult(HornRule oneRule, String targetPredicate, RuleType type, String subjType,
      String objType,boolean isInstantiation) {
    final HornRuleResult result = new HornRuleResult();
    result.setOutputRule(oneRule);
    result.setTargetPredicate(targetPredicate);
    result.setType(type);
    if (isInstantiation==true) {
      result.setAllInstantiations(
              ruleInstantiation.instantiateRule(targetPredicate, oneRule, subjType, objType, type, maxInstantiationNumber));
      final Set<String> targetEntities = Sets.newHashSet();
      result.getAllInstantiations().forEach(r -> {
        targetEntities.add(r.getRuleSubject());
        targetEntities.add(r.getRuleObject());
      });
      result.setSorroundingGraph(graphGeneration.generateSorroundingGraph(targetEntities));
    }
    return result;
  }

  public Graph<String> generateGraph(List<String> entities) {
    return graphGeneration.generateSorroundingGraph(entities);
  }

  public Collection<String> getAllKBRelations() {
    return kbAnalysis.getAllPredicates();
  }

  public void setMaxInstantiationNumber(int number) {
    this.maxInstantiationNumber = number;
  }

  public void setMaxRuleLength(int length) {
    this.ruleDiscovery.setMaxRuleLength(length);
  }

}
