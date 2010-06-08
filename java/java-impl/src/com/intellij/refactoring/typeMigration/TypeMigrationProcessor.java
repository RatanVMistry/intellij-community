package com.intellij.refactoring.typeMigration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.typeMigration.ui.FailedConversionsDialog;
import com.intellij.refactoring.typeMigration.ui.MigrationPanel;
import com.intellij.refactoring.typeMigration.usageInfo.TypeMigrationUsageInfo;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.usageView.UsageViewManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TypeMigrationProcessor extends BaseRefactoringProcessor {

  private PsiElement[] myRoot;
  private final TypeMigrationRules myRules;
  private TypeMigrationLabeler myLabeler;

  public TypeMigrationProcessor(final Project project, final PsiElement root, final TypeMigrationRules rules) {
    this(project, new PsiElement[]{root}, rules);
  }

  public TypeMigrationProcessor(final Project project, final PsiElement[] roots, final TypeMigrationRules rules) {
    super(project);
    myRoot = roots;
    myRules = rules;
  }

  protected UsageViewDescriptor createUsageViewDescriptor(UsageInfo[] usages) {
    return new TypeMigrationViewDescriptor(myRoot[0]);
  }

  protected boolean preprocessUsages(Ref<UsageInfo[]> refUsages) {
    if (hasFailedConversions()) {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        throw new RuntimeException(StringUtil.join(myLabeler.getFailedConversionsReport(), "\n"));
      }
      FailedConversionsDialog dialog = new FailedConversionsDialog(myLabeler.getFailedConversionsReport(), myProject);
      dialog.show();
      if (!dialog.isOK()) {
        final int exitCode = dialog.getExitCode();
        prepareSuccessful();
        if (exitCode == FailedConversionsDialog.VIEW_USAGES_EXIT_CODE) {
          previewRefactoring(refUsages.get());
        }
        return false;
      }
    }
    prepareSuccessful();
    return true;
  }

  public boolean hasFailedConversions() {
    return myLabeler.hasFailedConversions();
  }

  @Override
  protected void previewRefactoring(final UsageInfo[] usages) {
    MigrationPanel panel = new MigrationPanel(myRoot[0], myLabeler, myProject, isPreviewUsages());
    String text;
    if (myRoot[0] instanceof PsiField) {
      text = "field \'" + ((PsiField)myRoot[0]).getName() + "\'";
    } else if (myRoot[0] instanceof PsiParameter) {
      text = "parameter \'" + ((PsiParameter)myRoot[0]).getName() + "\'";
    } else if (myRoot[0] instanceof PsiLocalVariable) {
      text = "variable \'" + ((PsiLocalVariable)myRoot[0]).getName() + "\'";
    } else if (myRoot[0] instanceof PsiMethod) {
      text = "method \'" + ((PsiMethod)myRoot[0]).getName() + "\' return";
    } else {
      text = myRoot.toString();
    }
    Content content =  UsageViewManager.getInstance(myProject)
        .addContent("Migrate Type of " +
                    text +
                    " from \'" +
                    TypeMigrationLabeler.getElementType(myRoot[0]).getPresentableText() +
                    "\' to \'" +
                    myRules.getMigrationRootType().getPresentableText() +
                    "\'", false, panel, true, true);
    panel.setContent(content);
    ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.FIND).activate(null);
  }

  @NotNull
  public UsageInfo[] findUsages() {
    myLabeler = new TypeMigrationLabeler(myRules);

    try {
      return myLabeler.getMigratedUsages(!isPreviewUsages(), myRoot);
    }
    catch (TypeMigrationLabeler.MigrateException e) {
      setPreviewUsages(true);
      return myLabeler.getMigratedUsages(false, myRoot);
    }
  }

  protected void refreshElements(PsiElement[] elements) {
    myRoot = elements;
  }

  public void performRefactoring(UsageInfo[] usages) {
    change(myLabeler, usages);
  }

  public static void change(TypeMigrationLabeler labeler, UsageInfo[] usages) {
    List<UsageInfo> nonCodeUsages = new ArrayList<UsageInfo>();
    for (UsageInfo usage : usages) {
      if (((TypeMigrationUsageInfo)usage).isExcluded()) continue;
      final PsiElement element = usage.getElement();
      if (element instanceof PsiVariable || element instanceof PsiMember || element instanceof PsiExpression || element instanceof PsiReferenceParameterList) {
        labeler.change((TypeMigrationUsageInfo)usage);
      } else {
        nonCodeUsages.add(usage);
      }
    }
    for (UsageInfo usageInfo : nonCodeUsages) {
      final PsiElement element = usageInfo.getElement();
      if (element != null) {
        final PsiReference reference = element.getReference();
        if (reference != null) {
          final Object target = labeler.getConversion(element);
          if (target instanceof PsiMember) {
            try {
              reference.bindToElement((PsiElement)target);
            }
            catch (IncorrectOperationException e) {
              //skip
            }
          }
        }
      }
    }
  }

  public TypeMigrationLabeler getLabeler() {
    return myLabeler;
  }

  protected String getCommandName() {
    return "TypeMigration";
  }
}
