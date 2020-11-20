package io.github.f2bb.stripper;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.utils.SourceRoot;
import io.github.f2bb.Hide;
import io.github.f2bb.stripper.impl.Replacer;

public class StripSource {
	public static void main(String[] args) throws IOException {
		SourceRoot root = new SourceRoot(new File(args[0]).toPath());
		root.tryToParseParallelized().stream()
		    .peek(p -> p.getProblems().stream().map(Problem::getVerboseMessage).forEach(System.err::println))
		    .map(ParseResult::getResult).filter(Optional::isPresent).map(Optional::get)
		    .forEach(c -> c.accept(new Replacer(), null));
		root.saveAll(new File("out").toPath());
	}

	public static <T extends NodeWithModifiers<?> & NodeWithAnnotations<?>> boolean expose(T modifiers) {
		return (modifiers.hasModifier(Modifier.Keyword.PROTECTED) || modifiers
				                                                             .hasModifier(Modifier.Keyword.PUBLIC)) && !modifiers
						                                                                                                        .isAnnotationPresent(
								                                                                                                        Hide.class);
	}
}
