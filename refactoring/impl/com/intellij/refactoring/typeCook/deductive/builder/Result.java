package com.intellij.refactoring.typeCook.deductive.builder;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.typeCook.Settings;
import com.intellij.refactoring.typeCook.Util;
import com.intellij.refactoring.typeCook.deductive.resolver.Binding;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author db
 */
public class Result {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.typeCook.deductive.builder.Result");

  private final HashSet<PsiElement> myVictims;
  private final HashMap<PsiElement, PsiType> myTypes;
  private final Settings mySettings;
  private final HashMap<PsiTypeCastExpression, PsiType> myCastToOperandType;

  private int myCookedNumber = -1;
  private int myCastsRemoved = -1;
  private final int myCastsNumber;

  private Binding myBinding;

  public Result(final ReductionSystem system) {
    myVictims = system.myElements;
    myTypes = system.myTypes;
    mySettings = system.mySettings;
    myCastToOperandType = system.myCastToOperandType;
    myCastsNumber = myCastToOperandType.size();
  }

  public void incorporateSolution(final Binding binding) {
    if (myBinding == null) {
      myBinding = binding;
    }
    else {
      myBinding.merge(binding, mySettings.leaveObjectParameterizedTypesRaw());
    }
  }

  public PsiType getCookedType(final PsiElement element) {
    final PsiType originalType = Util.getType(element);

    if (myBinding != null) {
      final PsiType type = myBinding.substitute(myTypes.get(element));

      @NonNls final String objectFQName = "java.lang.Object";
      if (originalType.getCanonicalText().equals(objectFQName)) {
        if (type == null) {
          return originalType;
        }

        if (type instanceof PsiWildcardType){
          final PsiType bound = ((PsiWildcardType)type).getBound();

          if (bound != null){
            return bound;
          }

          return originalType;
        }
      }

      return type;
    }

    return originalType;
  }

  public HashSet<PsiElement> getCookedElements() {
    myCookedNumber = 0;

    final HashSet<PsiElement> set = new HashSet<PsiElement>();

    for (final PsiElement element : myVictims) {
      final PsiType originalType = Util.getType(element);

      final PsiType cookedType = getCookedType(element);
      if (cookedType != null && !originalType.equals(cookedType)) {
        set.add(element);
        myCookedNumber++;
      }
    }

    if (mySettings.dropObsoleteCasts()) {
      myCastsRemoved = 0;
      if (myBinding != null) {
        for (final Map.Entry<PsiTypeCastExpression,PsiType> entry : myCastToOperandType.entrySet()) {
          final PsiTypeCastExpression cast = entry.getKey();
          final PsiType operandType = myBinding.apply(entry.getValue());
          if (!(operandType instanceof PsiTypeVariable) && cast.getType().isAssignableFrom(operandType)) {
            set.add(cast);
          }
        }
      }
    }

    return set;
  }

  public void apply(final HashSet<PsiElement> victims) {
    for (final PsiElement element : victims) {
      if (element instanceof PsiTypeCastExpression && myCastToOperandType.containsKey(element)) {
        final PsiTypeCastExpression cast = ((PsiTypeCastExpression)element);
        try {
          cast.replace(cast.getOperand());
          myCastsRemoved++;
        }
        catch (IncorrectOperationException e1) {
          LOG.error(e1);
        }

      } else {
        Util.changeType(element, getCookedType(element));
      }
    }
  }

  private String getRatio(final int x, final int y) {
    final String ratio = RefactoringBundle.message("type.cook.ratio.generified", x, y);
    return ratio + (y != 0 ? " (" + (x * 100 / y) + "%)" : "");
  }

  public String getReport() {
    return RefactoringBundle.message("type.cook.report", getRatio(myCookedNumber, myVictims.size()),
                                     getRatio(myCastsRemoved, myCastsNumber));
  }
}
