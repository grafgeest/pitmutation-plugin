package org.jenkinsci.plugins.pitmutation.targets;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import hudson.util.TextFile;
import org.jenkinsci.plugins.pitmutation.Mutation;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ed Kimber
 */
public class MutatedClass extends MutationResult<MutatedClass> {

  public MutatedClass(String name, MutationResult parent, Collection<Mutation> mutations) {
    super(name, parent);
    name_ = name;
    mutations_ = mutations;

    int lastDot = name.lastIndexOf('.');
    int firstDollar = name.indexOf('$');
    package_ = lastDot >= 0 ? name.substring(0, lastDot) : "";
    fileName_ = firstDollar >= 0
      ? lastDot >= 0 ? name.substring(lastDot + 1, firstDollar) + ".java.html" : ""
      : lastDot >= 0 ? name.substring(lastDot + 1) + ".java.html" : "";

    mutatedLines_ = createMutatedLines(mutations);
  }

  private Map<String, MutatedLine> createMutatedLines(Collection<Mutation> mutations) {
    HashMultimap<String, Mutation> multimap = HashMultimap.create();
    for (Mutation m : mutations) {
      multimap.put(String.valueOf(m.getLineNumber()), m);
    }
    return Maps.transformEntries(multimap.asMap(), lineTransformer_);
  }

  @Override
  public boolean isSourceLevel() {
    return true;
  }

  public String getSourceFileContent() {
    try {
      return new TextFile(new File(getOwner().getRootDir(), "mutation-report-" + getParent().getParent().getName() + "/" + package_ + File.separator + fileName_)).read();
    } catch (IOException exception) {
      return "Could not read source file: " + getOwner().getRootDir().getPath()
        + "/mutation-report/" + package_ + File.separator + fileName_ + "\n";
    }
  }

  public String getDisplayName() {
    return "Class: " + getName();
  }

  @Override
  public MutationStats getMutationStats() {
    return new MutationStatsImpl(getName(), mutations_);
  }

  public Map<String, ? extends MutationResult<?>> getChildMap() {
    return mutatedLines_;
  }

  private final Maps.EntryTransformer<String, Collection<Mutation>, MutatedLine> lineTransformer_ =
    new Maps.EntryTransformer<String, Collection<Mutation>, MutatedLine>() {
      public MutatedLine transformEntry(String line, Collection<Mutation> mutations) {
        return new MutatedLine(line, MutatedClass.this, mutations);
      }
    };

  public String getName() {
    return name_;
  }

  public String getFileName() {
    return fileName_;
  }

  public String getPackage() {
    return package_;
  }

  @Override
  public int compareTo(@Nonnull MutatedClass other) {
    return this.getMutationStats().getUndetected() - other.getMutationStats().getUndetected();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof MutatedClass
      && Objects.equals(this.getMutationStats(), ((MutatedClass) other).getMutationStats())
      && Objects.equals(this.getChildMap(), ((MutatedClass) other).getChildMap())
      && Objects.equals(this.getDisplayName(), ((MutatedClass) other).getDisplayName())
      && Objects.equals(this.getFileName(), ((MutatedClass) other).getFileName())
      && Objects.equals(this.getUrl(), ((MutatedClass) other).getUrl())
      && Objects.equals(this.getSourceFileContent(), ((MutatedClass) other).getSourceFileContent());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getMutationStats(), this.getChildMap(), this.getDisplayName(),
      this.getFileName(), this.getUrl(), this.getSourceFileContent());
  }

  private String name_;
  private String package_;
  private String fileName_;
  private Collection<Mutation> mutations_;
  private Map<String, MutatedLine> mutatedLines_;
}
