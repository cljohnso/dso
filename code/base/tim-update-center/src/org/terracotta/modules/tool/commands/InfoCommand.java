/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.terracotta.modules.tool.Module;
import org.terracotta.modules.tool.ModuleId;
import org.terracotta.modules.tool.Modules;

import com.google.inject.Inject;

import java.util.List;

public class InfoCommand extends AbstractCommand {

  private static final String OPTION_GROUPID = "groupid";
  
  private final Modules       modules;

  @Inject
  public InfoCommand(Modules modules) {
    this.modules = modules;
    assert modules != null;
    options.addOption(OPTION_GROUPID, OPTION_GROUPID, true,
                      "Use this option to qualify the name of the TIM you are looking for.");
  }

  public void execute(CommandLine cli) throws CommandException {
    out().println();

    List<String> args = cli.getArgList();
    if (args.isEmpty()) { throw new CommandException("You need to at least specify the name of the Integration Module."); }

    String artifactId = args.remove(0);
    String version = args.isEmpty() ? null : args.remove(0);
    String groupId = cli.getOptionValue(OPTION_GROUPID, ModuleId.DEFAULT_GROUPID);
    Module module = (version == null) ? modules.getLatest(groupId, artifactId) : modules.get(ModuleId
        .create(groupId, artifactId, version));

    if (module == null) {
      out().println("Unable to locate the Terracotta Integration Module named '" + artifactId + "'");
      out().println("It might be using a groupId other than '" + groupId + "'");
      if (!cli.hasOption(OPTION_GROUPID)) {
        out().println("Qualify the TIM you specified by using the --" + groupId + " option.");
      }
      out().println();
      return;
    }

    module.printDetails(out());
  }

}
