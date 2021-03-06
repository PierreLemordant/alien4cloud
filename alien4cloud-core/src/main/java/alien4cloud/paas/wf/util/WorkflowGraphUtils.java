package alien4cloud.paas.wf.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.elasticsearch.common.collect.Lists;

import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.Path;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.exception.InconsistentWorkflowException;

public class WorkflowGraphUtils {

    public static AbstractStep getRequiredStep(Workflow workflow, String stepId) {
        if (workflow.getSteps() == null) {
            throw new InconsistentWorkflowException("The workflow doesn't contain any steps");
        }
        AbstractStep step = workflow.getSteps().get(stepId);
        if (step == null) {
            throw new InconsistentWorkflowException(String.format("The workflow doesn't contains the expected step <%s> !", stepId));
        }
        return step;
    }

    /**
     * Build the paths of the graph starting from the entry points (steps without predecessors, so connected to 'start').
     * <p>
     * Will also detect orphans brothers in the entire graph (cycles not connected to start).
     */
    public static List<Path> getWorkflowGraphPaths(Workflow workflow) {
        // the result
        List<Path> allPaths = new ArrayList<Path>();
        // find the entry steps
        Set<AbstractStep> graphEntries = getGraphEntrySteps(workflow);
        // all the steps: this set will be cleared while browsing the graph and will able us to detect orphans
        Set<AbstractStep> allSteps = new HashSet<AbstractStep>(workflow.getSteps().values());
        for (AbstractStep graphEntry : graphEntries) {
            List<Path> initialPaths = new ArrayList<Path>();
            // we have 1 initial path : it's the 'start'
            Path startPath = new Path();
            initialPaths.add(startPath);
            recursivelyPopulatePaths(workflow, allPaths, initialPaths, graphEntry, allSteps);
        }
        while (!allSteps.isEmpty()) {
            // we have orphans steps that are not on any path, in cycles
            // so just peek the first one, and build paths until this set is empty
            AbstractStep step = allSteps.iterator().next();
            List<Path> initialPaths = new ArrayList<Path>();
            // initial path
            Path startPath = new Path();
            initialPaths.add(startPath);
            recursivelyPopulatePaths(workflow, allPaths, initialPaths, step, allSteps);
        }
        return allPaths;
    }

    /**
     * TODO: is this a tail recursive fn ?
     */
    private static void recursivelyPopulatePaths(Workflow workflow, List<Path> allPaths, List<Path> predecessors, AbstractStep step, Set<AbstractStep> allSteps) {
        boolean stepHasFollowers = step.getFollowingSteps() != null && !step.getFollowingSteps().isEmpty();
        for (Path path : predecessors) {
            if (path.contains(step)) {
                // the step is already in one of it's parent path, we have a cycle
                path.setCycle(true);
                path.setLoopingStep(step);
            } else {
                // a step is added to it's predecessor paths
                path.add(step);
                // since the step is on a path, we remove it from the allSteps set
                allSteps.remove(step);
            }
            if (path.isCycle() || !stepHasFollowers) {
                // this is the end for this path
                allPaths.add(path);
            }
        }
        if (stepHasFollowers) {
            for (String followingId : step.getFollowingSteps()) {
                AbstractStep followingStep = WorkflowGraphUtils.getRequiredStep(workflow, followingId);
                List<Path> followersPredecessors = Lists.newArrayList();
                for (Path path : predecessors) {
                    if (!path.isCycle()) {
                        Path clone = new Path(path);
                        followersPredecessors.add(clone);
                    }
                }
                if (followersPredecessors.size() > 0) {
                    recursivelyPopulatePaths(workflow, allPaths, followersPredecessors, followingStep, allSteps);
                }
            }
        }
    }

    /**
     * The graph entry is the lists of steps that haven't predecessor: these steps are de-facto connected to 'start'.
     */
    public static Set<AbstractStep> getGraphEntrySteps(Workflow workflow) {
        Set<AbstractStep> entries = new HashSet<AbstractStep>();
        for (AbstractStep step : workflow.getSteps().values()) {
            if (step.getPrecedingSteps() == null || step.getPrecedingSteps().isEmpty()) {
                entries.add(step);
            }
        }
        return entries;
    }

}
