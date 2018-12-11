package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.extraction.java.NameResolver;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class CodeAnalyzer {
    private String code;
    public CodeAnalyzer(String code){
        this.code=code;
    }
    public String getFullNameFromCode(){
        ASTParser parser = ASTParser.newParser(AST.JLS10);
        parser.setSource(this.code.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        ASTVisitor codeVisitor=new CodeVisitor();
        CompilationUnit unit=(CompilationUnit)parser.createAST(null);
        unit.accept(codeVisitor);
        return ((CodeVisitor) codeVisitor).getClassName();
    }

    class CodeVisitor extends ASTVisitor{
        private List<String> className=null;
        public String getClassName(){
            if(this.className!=null)
                return className.get(0);
            else return null;
        }
        public boolean visit(TypeDeclaration node){
            if(this.className==null){
                this.className=new ArrayList<String>();
                this.className.add(NameResolver.getFullName(node));
            }else{
                this.className.add(NameResolver.getFullName(node));
            }
            return false;
        }
    }

    public static void main(String args[]){
        String code="/*\n" +
                " * Licensed to the Apache Software Foundation (ASF) under one or more\n" +
                " * contributor license agreements.  See the NOTICE file distributed with\n" +
                " * this work for additional information regarding copyright ownership.\n" +
                " * The ASF licenses this file to You under the Apache License, Version 2.0\n" +
                " * (the \"License\"); you may not use this file except in compliance with\n" +
                " * the License.  You may obtain a copy of the License at\n" +
                " *\n" +
                " *     http://www.apache.org/licenses/LICENSE-2.0\n" +
                " *\n" +
                " * Unless required by applicable law or agreed to in writing, software\n" +
                " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                " * See the License for the specific language governing permissions and\n" +
                " * limitations under the License.\n" +
                " */\n" +
                "package org.apache.lucene.index;\n" +
                "import java.io.Closeable;\n" +
                "import java.io.IOException;\n" +
                "import java.util.Collections;\n" +
                "import java.util.LinkedHashSet;\n" +
                "import java.util.List;\n" +
                "import java.util.Set;\n" +
                "import java.util.WeakHashMap;\n" +
                "import java.util.concurrent.atomic.AtomicInteger;\n" +
                "import org.apache.lucene.document.Document;\n" +
                "import org.apache.lucene.document.DocumentStoredFieldVisitor;\n" +
                "import org.apache.lucene.store.AlreadyClosedException;\n" +
                "import org.apache.lucene.util.Bits;  // javadocs\n" +
                "import org.apache.lucene.util.IOUtils;\n" +
                "/**\n" +
                " IndexReader is an abstract class, providing an interface for accessing a\n" +
                " point-in-time view of an index.  Any changes made to the index\n" +
                " via {@link IndexWriter} will not be visible until a new\n" +
                " {@code IndexReader} is opened.  It's best to use {@link\n" +
                " DirectoryReader#open(IndexWriter)} to obtain an\n" +
                " {@code IndexReader}, if your {@link IndexWriter} is\n" +
                " in-process.  When you need to re-open to see changes to the\n" +
                " index, it's best to use {@link DirectoryReader#openIfChanged(DirectoryReader)}\n" +
                " since the new reader will share resources with the previous\n" +
                " one when possible.  Search of an index is done entirely\n" +
                " through this abstract interface, so that any subclass which\n" +
                " implements it is searchable.\n" +
                " <p>There are two different types of IndexReaders:\n" +
                " <ul>\n" +
                "  <li>{@link LeafReader}: These indexes do not consist of several sub-readers,\n" +
                "  they are atomic. They support retrieval of stored fields, doc values, terms,\n" +
                "  and postings.\n" +
                "  <li>{@link CompositeReader}: Instances (like {@link DirectoryReader})\n" +
                "  of this reader can only\n" +
                "  be used to get stored fields from the underlying LeafReaders,\n" +
                "  but it is not possible to directly retrieve postings. To do that, get\n" +
                "  the sub-readers via {@link CompositeReader#getSequentialSubReaders}.\n" +
                " </ul>\n" +
                " \n" +
                " <p>IndexReader instances for indexes on disk are usually constructed\n" +
                " with a call to one of the static <code>DirectoryReader.open()</code> methods,\n" +
                " e.g. {@link DirectoryReader#open(org.apache.lucene.store.Directory)}. {@link DirectoryReader} implements\n" +
                " the {@link CompositeReader} interface, it is not possible to directly get postings.\n" +
                " <p> For efficiency, in this API documents are often referred to via\n" +
                " <i>document numbers</i>, non-negative integers which each name a unique\n" +
                " document in the index.  These document numbers are ephemeral -- they may change\n" +
                " as documents are added to and deleted from an index.  Clients should thus not\n" +
                " rely on a given document having the same number between sessions.\n" +
                " <p>\n" +
                " <a name=\"thread-safety\"></a><p><b>NOTE</b>: {@link\n" +
                " IndexReader} instances are completely thread\n" +
                " safe, meaning multiple threads can call any of its methods,\n" +
                " concurrently.  If your application requires external\n" +
                " synchronization, you should <b>not</b> synchronize on the\n" +
                " <code>IndexReader</code> instance; use your own\n" +
                " (non-Lucene) objects instead.\n" +
                "*/\n" +
                "public abstract class IndexReader implements Closeable {\n" +
                "  \n" +
                "  private boolean closed = false;\n" +
                "  private boolean closedByChild = false;\n" +
                "  private final AtomicInteger refCount = new AtomicInteger(1);\n" +
                "  IndexReader() {\n" +
                "    if (!(this instanceof CompositeReader || this instanceof LeafReader))\n" +
                "      throw new Error(\"IndexReader should never be directly extended, subclass LeafReader or CompositeReader instead.\");\n" +
                "  }\n" +
                "  \n" +
                "  /**\n" +
                "   * A custom listener that's invoked when the IndexReader\n" +
                "   * is closed.\n" +
                "   *\n" +
                "   * @lucene.experimental\n" +
                "   */\n" +
                "  public static interface ReaderClosedListener {\n" +
                "    /** Invoked when the {@link IndexReader} is closed. */\n" +
                "    public void onClose(IndexReader reader) throws IOException;\n" +
                "  }\n" +
                "  private final Set<ReaderClosedListener> readerClosedListeners = \n" +
                "      Collections.synchronizedSet(new LinkedHashSet<ReaderClosedListener>());\n" +
                "  private final Set<IndexReader> parentReaders = \n" +
                "      Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<IndexReader,Boolean>()));\n" +
                "  /** Expert: adds a {@link ReaderClosedListener}.  The\n" +
                "   * provided listener will be invoked when this reader is closed.\n" +
                "   * At this point, it is safe for apps to evict this reader from\n" +
                "   * any caches keyed on {@link #getCombinedCoreAndDeletesKey()}.\n" +
                "   *\n" +
                "   * @lucene.experimental */\n" +
                "  public final void addReaderClosedListener(ReaderClosedListener listener) {\n" +
                "    ensureOpen();\n" +
                "    readerClosedListeners.add(listener);\n" +
                "  }\n" +
                "  /** Expert: remove a previously added {@link ReaderClosedListener}.\n" +
                "   *\n" +
                "   * @lucene.experimental */\n" +
                "  public final void removeReaderClosedListener(ReaderClosedListener listener) {\n" +
                "    ensureOpen();\n" +
                "    readerClosedListeners.remove(listener);\n" +
                "  }\n" +
                "  \n" +
                "  /** Expert: This method is called by {@code IndexReader}s which wrap other readers\n" +
                "   * (e.g. {@link CompositeReader} or {@link FilterLeafReader}) to register the parent\n" +
                "   * at the child (this reader) on construction of the parent. When this reader is closed,\n" +
                "   * it will mark all registered parents as closed, too. The references to parent readers\n" +
                "   * are weak only, so they can be GCed once they are no longer in use.\n" +
                "   * @lucene.experimental */\n" +
                "  public final void registerParentReader(IndexReader reader) {\n" +
                "    ensureOpen();\n" +
                "    parentReaders.add(reader);\n" +
                "  }\n" +
                "  private void notifyReaderClosedListeners(Throwable th) throws IOException {\n" +
                "    synchronized(readerClosedListeners) {\n" +
                "      for(ReaderClosedListener listener : readerClosedListeners) {\n" +
                "        try {\n" +
                "          listener.onClose(this);\n" +
                "        } catch (Throwable t) {\n" +
                "          if (th == null) {\n" +
                "            th = t;\n" +
                "          } else {\n" +
                "            th.addSuppressed(t);\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "      IOUtils.reThrow(th);\n" +
                "    }\n" +
                "  }\n" +
                "  private void reportCloseToParentReaders() {\n" +
                "    synchronized(parentReaders) {\n" +
                "      for(IndexReader parent : parentReaders) {\n" +
                "        parent.closedByChild = true;\n" +
                "        // cross memory barrier by a fake write:\n" +
                "        parent.refCount.addAndGet(0);\n" +
                "        // recurse:\n" +
                "        parent.reportCloseToParentReaders();\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "  /** Expert: returns the current refCount for this reader */\n" +
                "  public final int getRefCount() {\n" +
                "    // NOTE: don't ensureOpen, so that callers can see\n" +
                "    // refCount is 0 (reader is closed)\n" +
                "    return refCount.get();\n" +
                "  }\n" +
                "  \n" +
                "  /**\n" +
                "   * Expert: increments the refCount of this IndexReader\n" +
                "   * instance.  RefCounts are used to determine when a\n" +
                "   * reader can be closed safely, i.e. as soon as there are\n" +
                "   * no more references.  Be sure to always call a\n" +
                "   * corresponding {@link #decRef}, in a finally clause;\n" +
                "   * otherwise the reader may never be closed.  Note that\n" +
                "   * {@link #close} simply calls decRef(), which means that\n" +
                "   * the IndexReader will not really be closed until {@link\n" +
                "   * #decRef} has been called for all outstanding\n" +
                "   * references.\n" +
                "   *\n" +
                "   * @see #decRef\n" +
                "   * @see #tryIncRef\n" +
                "   */\n" +
                "  public final void incRef() {\n" +
                "    if (!tryIncRef()) {\n" +
                "      ensureOpen();\n" +
                "    }\n" +
                "  }\n" +
                "  \n" +
                "  /**\n" +
                "   * Expert: increments the refCount of this IndexReader\n" +
                "   * instance only if the IndexReader has not been closed yet\n" +
                "   * and returns <code>true</code> iff the refCount was\n" +
                "   * successfully incremented, otherwise <code>false</code>.\n" +
                "   * If this method returns <code>false</code> the reader is either\n" +
                "   * already closed or is currently being closed. Either way this\n" +
                "   * reader instance shouldn't be used by an application unless\n" +
                "   * <code>true</code> is returned.\n" +
                "   * <p>\n" +
                "   * RefCounts are used to determine when a\n" +
                "   * reader can be closed safely, i.e. as soon as there are\n" +
                "   * no more references.  Be sure to always call a\n" +
                "   * corresponding {@link #decRef}, in a finally clause;\n" +
                "   * otherwise the reader may never be closed.  Note that\n" +
                "   * {@link #close} simply calls decRef(), which means that\n" +
                "   * the IndexReader will not really be closed until {@link\n" +
                "   * #decRef} has been called for all outstanding\n" +
                "   * references.\n" +
                "   *\n" +
                "   * @see #decRef\n" +
                "   * @see #incRef\n" +
                "   */\n" +
                "  public final boolean tryIncRef() {\n" +
                "    int count;\n" +
                "    while ((count = refCount.get()) > 0) {\n" +
                "      if (refCount.compareAndSet(count, count+1)) {\n" +
                "        return true;\n" +
                "      }\n" +
                "    }\n" +
                "    return false;\n" +
                "  }\n" +
                "  /**\n" +
                "   * Expert: decreases the refCount of this IndexReader\n" +
                "   * instance.  If the refCount drops to 0, then this\n" +
                "   * reader is closed.  If an exception is hit, the refCount\n" +
                "   * is unchanged.\n" +
                "   *\n" +
                "   * @throws IOException in case an IOException occurs in  doClose()\n" +
                "   *\n" +
                "   * @see #incRef\n" +
                "   */\n" +
                "  public final void decRef() throws IOException {\n" +
                "    // only check refcount here (don't call ensureOpen()), so we can\n" +
                "    // still close the reader if it was made invalid by a child:\n" +
                "    if (refCount.get() <= 0) {\n" +
                "      throw new AlreadyClosedException(\"this IndexReader is closed\");\n" +
                "    }\n" +
                "    \n" +
                "    final int rc = refCount.decrementAndGet();\n" +
                "    if (rc == 0) {\n" +
                "      closed = true;\n" +
                "      Throwable throwable = null;\n" +
                "      try {\n" +
                "        doClose();\n" +
                "      } catch (Throwable th) {\n" +
                "        throwable = th;\n" +
                "      } finally {\n" +
                "        try {\n" +
                "          reportCloseToParentReaders();\n" +
                "        } finally {\n" +
                "          notifyReaderClosedListeners(throwable);\n" +
                "        }\n" +
                "      }\n" +
                "    } else if (rc < 0) {\n" +
                "      throw new IllegalStateException(\"too many decRef calls: refCount is \" + rc + \" after decrement\");\n" +
                "    }\n" +
                "  }\n" +
                "  \n" +
                "  /**\n" +
                "   * Throws AlreadyClosedException if this IndexReader or any\n" +
                "   * of its child readers is closed, otherwise returns.\n" +
                "   */\n" +
                "  protected final void ensureOpen() throws AlreadyClosedException {\n" +
                "    if (refCount.get() <= 0) {\n" +
                "      throw new AlreadyClosedException(\"this IndexReader is closed\");\n" +
                "    }\n" +
                "    // the happens before rule on reading the refCount, which must be after the fake write,\n" +
                "    // ensures that we see the value:\n" +
                "    if (closedByChild) {\n" +
                "      throw new AlreadyClosedException(\"this IndexReader cannot be used anymore as one of its child readers was closed\");\n" +
                "    }\n" +
                "  }\n" +
                "  \n" +
                "  /** {@inheritDoc}\n" +
                "   * <p>For caching purposes, {@code IndexReader} subclasses are not allowed\n" +
                "   * to implement equals/hashCode, so methods are declared final.\n" +
                "   * To lookup instances from caches use {@link #getCoreCacheKey} and \n" +
                "   * {@link #getCombinedCoreAndDeletesKey}.\n" +
                "   */\n" +
                "  @Override\n" +
                "  public final boolean equals(Object obj) {\n" +
                "    return (this == obj);\n" +
                "  }\n" +
                "  \n" +
                "  /** {@inheritDoc}\n" +
                "   * <p>For caching purposes, {@code IndexReader} subclasses are not allowed\n" +
                "   * to implement equals/hashCode, so methods are declared final.\n" +
                "   * To lookup instances from caches use {@link #getCoreCacheKey} and \n" +
                "   * {@link #getCombinedCoreAndDeletesKey}.\n" +
                "   */\n" +
                "  @Override\n" +
                "  public final int hashCode() {\n" +
                "    return System.identityHashCode(this);\n" +
                "  }\n" +
                "  /** Retrieve term vectors for this document, or null if\n" +
                "   *  term vectors were not indexed.  The returned Fields\n" +
                "   *  instance acts like a single-document inverted index\n" +
                "   *  (the docID will be 0). */\n" +
                "  public abstract Fields getTermVectors(int docID)\n" +
                "          throws IOException;\n" +
                "  /** Retrieve term vector for this document and field, or\n" +
                "   *  null if term vectors were not indexed.  The returned\n" +
                "   *  Fields instance acts like a single-document inverted\n" +
                "   *  index (the docID will be 0). */\n" +
                "  public final Terms getTermVector(int docID, String field)\n" +
                "    throws IOException {\n" +
                "    Fields vectors = getTermVectors(docID);\n" +
                "    if (vectors == null) {\n" +
                "      return null;\n" +
                "    }\n" +
                "    return vectors.terms(field);\n" +
                "  }\n" +
                "  /** Returns the number of documents in this index. */\n" +
                "  public abstract int numDocs();\n" +
                "  /** Returns one greater than the largest possible document number.\n" +
                "   * This may be used to, e.g., determine how big to allocate an array which\n" +
                "   * will have an element for every document number in an index.\n" +
                "   */\n" +
                "  public abstract int maxDoc();\n" +
                "  /** Returns the number of deleted documents. */\n" +
                "  public final int numDeletedDocs() {\n" +
                "    return maxDoc() - numDocs();\n" +
                "  }\n" +
                "  /** Expert: visits the fields of a stored document, for\n" +
                "   *  custom processing/loading of each field.  If you\n" +
                "   *  simply want to load all fields, use {@link\n" +
                "   *  #document(int)}.  If you want to load a subset, use\n" +
                "   *  {@link DocumentStoredFieldVisitor}.  */\n" +
                "  public abstract void document(int docID, StoredFieldVisitor visitor) throws IOException;\n" +
                "  \n" +
                "  /**\n" +
                "   * Returns the stored fields of the <code>n</code><sup>th</sup>\n" +
                "   * <code>Document</code> in this index.  This is just\n" +
                "   * sugar for using {@link DocumentStoredFieldVisitor}.\n" +
                "   * <p>\n" +
                "   * <b>NOTE:</b> for performance reasons, this method does not check if the\n" +
                "   * requested document is deleted, and therefore asking for a deleted document\n" +
                "   * may yield unspecified results. Usually this is not required, however you\n" +
                "   * can test if the doc is deleted by checking the {@link\n" +
                "   * Bits} returned from {@link MultiFields#getLiveDocs}.\n" +
                "   *\n" +
                "   * <b>NOTE:</b> only the content of a field is returned,\n" +
                "   * if that field was stored during indexing.  Metadata\n" +
                "   * like boost, omitNorm, IndexOptions, tokenized, etc.,\n" +
                "   * are not preserved.\n" +
                "   * \n" +
                "   * @throws CorruptIndexException if the index is corrupt\n" +
                "   * @throws IOException if there is a low-level IO error\n" +
                "   */\n" +
                "  // TODO: we need a separate StoredField, so that the\n" +
                "  // Document returned here contains that class not\n" +
                "  // IndexableField\n" +
                "  public final Document document(int docID) throws IOException {\n" +
                "    final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();\n" +
                "    document(docID, visitor);\n" +
                "    return visitor.getDocument();\n" +
                "  }\n" +
                "  /**\n" +
                "   * Like {@link #document(int)} but only loads the specified\n" +
                "   * fields.  Note that this is simply sugar for {@link\n" +
                "   * DocumentStoredFieldVisitor#DocumentStoredFieldVisitor(Set)}.\n" +
                "   */\n" +
                "  public final Document document(int docID, Set<String> fieldsToLoad)\n" +
                "      throws IOException {\n" +
                "    final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor(\n" +
                "        fieldsToLoad);\n" +
                "    document(docID, visitor);\n" +
                "    return visitor.getDocument();\n" +
                "  }\n" +
                "  /** Returns true if any documents have been deleted. Implementers should\n" +
                "   *  consider overriding this method if {@link #maxDoc()} or {@link #numDocs()}\n" +
                "   *  are not constant-time operations. */\n" +
                "  public boolean hasDeletions() {\n" +
                "    return numDeletedDocs() > 0;\n" +
                "  }\n" +
                "  /**\n" +
                "   * Closes files associated with this index.\n" +
                "   * Also saves any new deletions to disk.\n" +
                "   * No other methods should be called after this has been called.\n" +
                "   * @throws IOException if there is a low-level IO error\n" +
                "   */\n" +
                "  @Override\n" +
                "  public final synchronized void close() throws IOException {\n" +
                "    if (!closed) {\n" +
                "      decRef();\n" +
                "      closed = true;\n" +
                "    }\n" +
                "  }\n" +
                "  \n" +
                "  /** Implements close. */\n" +
                "  protected abstract void doClose() throws IOException;\n" +
                "  /**\n" +
                "   * Expert: Returns the root {@link IndexReaderContext} for this\n" +
                "   * {@link IndexReader}'s sub-reader tree. \n" +
                "   * <p>\n" +
                "   * Iff this reader is composed of sub\n" +
                "   * readers, i.e. this reader being a composite reader, this method returns a\n" +
                "   * {@link CompositeReaderContext} holding the reader's direct children as well as a\n" +
                "   * view of the reader tree's atomic leaf contexts. All sub-\n" +
                "   * {@link IndexReaderContext} instances referenced from this readers top-level\n" +
                "   * context are private to this reader and are not shared with another context\n" +
                "   * tree. For example, IndexSearcher uses this API to drive searching by one\n" +
                "   * atomic leaf reader at a time. If this reader is not composed of child\n" +
                "   * readers, this method returns an {@link LeafReaderContext}.\n" +
                "   * <p>\n" +
                "   * Note: Any of the sub-{@link CompositeReaderContext} instances referenced\n" +
                "   * from this top-level context do not support {@link CompositeReaderContext#leaves()}.\n" +
                "   * Only the top-level context maintains the convenience leaf-view\n" +
                "   * for performance reasons.\n" +
                "   */\n" +
                "  public abstract IndexReaderContext getContext();\n" +
                "  \n" +
                "  /**\n" +
                "   * Returns the reader's leaves, or itself if this reader is atomic.\n" +
                "   * This is a convenience method calling {@code this.getContext().leaves()}.\n" +
                "   * @see IndexReaderContext#leaves()\n" +
                "   */\n" +
                "  public final List<LeafReaderContext> leaves() {\n" +
                "    return getContext().leaves();\n" +
                "  }\n" +
                "  /** Expert: Returns a key for this IndexReader, so CachingWrapperFilter can find\n" +
                "   * it again.\n" +
                "   * This key must not have equals()/hashCode() methods, so &quot;equals&quot; means &quot;identical&quot;. */\n" +
                "  public Object getCoreCacheKey() {\n" +
                "    // Don't call ensureOpen since FC calls this (to evict)\n" +
                "    // on close\n" +
                "    return this;\n" +
                "  }\n" +
                "  /** Expert: Returns a key for this IndexReader that also includes deletions,\n" +
                "   * so CachingWrapperFilter can find it again.\n" +
                "   * This key must not have equals()/hashCode() methods, so &quot;equals&quot; means &quot;identical&quot;. */\n" +
                "  public Object getCombinedCoreAndDeletesKey() {\n" +
                "    // Don't call ensureOpen since FC calls this (to evict)\n" +
                "    // on close\n" +
                "    return this;\n" +
                "  }\n" +
                "  \n" +
                "  /** Returns the number of documents containing the \n" +
                "   * <code>term</code>.  This method returns 0 if the term or\n" +
                "   * field does not exists.  This method does not take into\n" +
                "   * account deleted documents that have not yet been merged\n" +
                "   * away. \n" +
                "   * @see TermsEnum#docFreq()\n" +
                "   */\n" +
                "  public abstract int docFreq(Term term) throws IOException;\n" +
                "  \n" +
                "  /**\n" +
                "   * Returns the total number of occurrences of {@code term} across all\n" +
                "   * documents (the sum of the freq() for each doc that has this term). This\n" +
                "   * will be -1 if the codec doesn't support this measure. Note that, like other\n" +
                "   * term measures, this measure does not take deleted documents into account.\n" +
                "   */\n" +
                "  public abstract long totalTermFreq(Term term) throws IOException;\n" +
                "  \n" +
                "  /**\n" +
                "   * Returns the sum of {@link TermsEnum#docFreq()} for all terms in this field,\n" +
                "   * or -1 if this measure isn't stored by the codec. Note that, just like other\n" +
                "   * term measures, this measure does not take deleted documents into account.\n" +
                "   * \n" +
                "   * @see Terms#getSumDocFreq()\n" +
                "   */\n" +
                "  public abstract long getSumDocFreq(String field) throws IOException;\n" +
                "  \n" +
                "  /**\n" +
                "   * Returns the number of documents that have at least one term for this field,\n" +
                "   * or -1 if this measure isn't stored by the codec. Note that, just like other\n" +
                "   * term measures, this measure does not take deleted documents into account.\n" +
                "   * \n" +
                "   * @see Terms#getDocCount()\n" +
                "   */\n" +
                "  public abstract int getDocCount(String field) throws IOException;\n" +
                "  /**\n" +
                "   * Returns the sum of {@link TermsEnum#totalTermFreq} for all terms in this\n" +
                "   * field, or -1 if this measure isn't stored by the codec (or if this fields\n" +
                "   * omits term freq and positions). Note that, just like other term measures,\n" +
                "   * this measure does not take deleted documents into account.\n" +
                "   * \n" +
                "   * @see Terms#getSumTotalTermFreq()\n" +
                "   */\n" +
                "  public abstract long getSumTotalTermFreq(String field) throws IOException;\n" +
                "}";
        String code1="/*  * Licensed to the Apache Software Foundation (ASF) under one or more  * contributor license agreements.  See the NOTICE file distributed with  * this work for additional information regarding copyright ownership.  * The ASF licenses this file to You under the Apache License, Version 2.0  * (the \"License\"); you may not use this file except in compliance with  * the License.  You may obtain a copy of the License at  *  *     http://www.apache.org/licenses/LICENSE-2.0  *  * Unless required by applicable law or agreed to in writing, software  * distributed under the License is distributed on an \"AS IS\" BASIS,  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  * See the License for the specific language governing permissions and  * limitations under the License.  */ package org.apache.lucene.index; import java.io.Closeable; import java.io.IOException; import java.util.Collections; import java.util.LinkedHashSet; import java.util.List; import java.util.Set; import java.util.WeakHashMap; import java.util.concurrent.atomic.AtomicInteger; import org.apache.lucene.document.Document; import org.apache.lucene.document.DocumentStoredFieldVisitor; import org.apache.lucene.store.AlreadyClosedException; import org.apache.lucene.util.Bits;  // javadocs import org.apache.lucene.util.IOUtils; /**  IndexReader is an abstract class, providing an interface for accessing a  point-in-time view of an index.  Any changes made to the index  via {@link IndexWriter} will not be visible until a new  {@code IndexReader} is opened.  It's best to use {@link  DirectoryReader#open(IndexWriter)} to obtain an  {@code IndexReader}, if your {@link IndexWriter} is  in-process.  When you need to re-open to see changes to the  index, it's best to use {@link DirectoryReader#openIfChanged(DirectoryReader)}  since the new reader will share resources with the previous  one when possible.  Search of an index is done entirely  through this abstract interface, so that any subclass which  implements it is searchable.  <p>There are two different types of IndexReaders:  <ul>   <li>{@link LeafReader}: These indexes do not consist of several sub-readers,   they are atomic. They support retrieval of stored fields, doc values, terms,   and postings.   <li>{@link CompositeReader}: Instances (like {@link DirectoryReader})   of this reader can only   be used to get stored fields from the underlying LeafReaders,   but it is not possible to directly retrieve postings. To do that, get   the sub-readers via {@link CompositeReader#getSequentialSubReaders}.  </ul>    <p>IndexReader instances for indexes on disk are usually constructed  with a call to one of the static <code>DirectoryReader.open()</code> methods,  e.g. {@link DirectoryReader#open(org.apache.lucene.store.Directory)}. {@link DirectoryReader} implements  the {@link CompositeReader} interface, it is not possible to directly get postings.  <p> For efficiency, in this API documents are often referred to via  <i>document numbers</i>, non-negative integers which each name a unique  document in the index.  These document numbers are ephemeral -- they may change  as documents are added to and deleted from an index.  Clients should thus not  rely on a given document having the same number between sessions.  <p>  <a name=\"thread-safety\"></a><p><b>NOTE</b>: {@link  IndexReader} instances are completely thread  safe, meaning multiple threads can call any of its methods,  concurrently.  If your application requires external  synchronization, you should <b>not</b> synchronize on the  <code>IndexReader</code> instance; use your own  (non-Lucene) objects instead. */ public abstract class IndexReader implements Closeable {      private boolean closed = false;   private boolean closedByChild = false;   private final AtomicInteger refCount = new AtomicInteger(1);   IndexReader() {     if (!(this instanceof CompositeReader || this instanceof LeafReader))       throw new Error(\"IndexReader should never be directly extended, subclass LeafReader or CompositeReader instead.\");   }      /**    * A custom listener that's invoked when the IndexReader    * is closed.    *    * @lucene.experimental    */   public static interface ReaderClosedListener {     /** Invoked when the {@link IndexReader} is closed. */     public void onClose(IndexReader reader) throws IOException;   }   private final Set<ReaderClosedListener> readerClosedListeners =        Collections.synchronizedSet(new LinkedHashSet<ReaderClosedListener>());   private final Set<IndexReader> parentReaders =        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<IndexReader,Boolean>()));   /** Expert: adds a {@link ReaderClosedListener}.  The    * provided listener will be invoked when this reader is closed.    * At this point, it is safe for apps to evict this reader from    * any caches keyed on {@link #getCombinedCoreAndDeletesKey()}.    *    * @lucene.experimental */   public final void addReaderClosedListener(ReaderClosedListener listener) {     ensureOpen();     readerClosedListeners.add(listener);   }   /** Expert: remove a previously added {@link ReaderClosedListener}.    *    * @lucene.experimental */   public final void removeReaderClosedListener(ReaderClosedListener listener) {     ensureOpen();     readerClosedListeners.remove(listener);   }      /** Expert: This method is called by {@code IndexReader}s which wrap other readers    * (e.g. {@link CompositeReader} or {@link FilterLeafReader}) to register the parent    * at the child (this reader) on construction of the parent. When this reader is closed,    * it will mark all registered parents as closed, too. The references to parent readers    * are weak only, so they can be GCed once they are no longer in use.    * @lucene.experimental */   public final void registerParentReader(IndexReader reader) {     ensureOpen();     parentReaders.add(reader);   }   private void notifyReaderClosedListeners(Throwable th) throws IOException {     synchronized(readerClosedListeners) {       for(ReaderClosedListener listener : readerClosedListeners) {         try {           listener.onClose(this);         } catch (Throwable t) {           if (th == null) {             th = t;           } else {             th.addSuppressed(t);           }         }       }       IOUtils.reThrow(th);     }   }   private void reportCloseToParentReaders() {     synchronized(parentReaders) {       for(IndexReader parent : parentReaders) {         parent.closedByChild = true;         // cross memory barrier by a fake write:         parent.refCount.addAndGet(0);         // recurse:         parent.reportCloseToParentReaders();       }     }   }   /** Expert: returns the current refCount for this reader */   public final int getRefCount() {     // NOTE: don't ensureOpen, so that callers can see     // refCount is 0 (reader is closed)     return refCount.get();   }      /**    * Expert: increments the refCount of this IndexReader    * instance.  RefCounts are used to determine when a    * reader can be closed safely, i.e. as soon as there are    * no more references.  Be sure to always call a    * corresponding {@link #decRef}, in a finally clause;    * otherwise the reader may never be closed.  Note that    * {@link #close} simply calls decRef(), which means that    * the IndexReader will not really be closed until {@link    * #decRef} has been called for all outstanding    * references.    *    * @see #decRef    * @see #tryIncRef    */   public final void incRef() {     if (!tryIncRef()) {       ensureOpen();     }   }      /**    * Expert: increments the refCount of this IndexReader    * instance only if the IndexReader has not been closed yet    * and returns <code>true</code> iff the refCount was    * successfully incremented, otherwise <code>false</code>.    * If this method returns <code>false</code> the reader is either    * already closed or is currently being closed. Either way this    * reader instance shouldn't be used by an application unless    * <code>true</code> is returned.    * <p>    * RefCounts are used to determine when a    * reader can be closed safely, i.e. as soon as there are    * no more references.  Be sure to always call a    * corresponding {@link #decRef}, in a finally clause;    * otherwise the reader may never be closed.  Note that    * {@link #close} simply calls decRef(), which means that    * the IndexReader will not really be closed until {@link    * #decRef} has been called for all outstanding    * references.    *    * @see #decRef    * @see #incRef    */   public final boolean tryIncRef() {     int count;     while ((count = refCount.get()) > 0) {       if (refCount.compareAndSet(count, count+1)) {         return true;       }     }     return false;   }   /**    * Expert: decreases the refCount of this IndexReader    * instance.  If the refCount drops to 0, then this    * reader is closed.  If an exception is hit, the refCount    * is unchanged.    *    * @throws IOException in case an IOException occurs in  doClose()    *    * @see #incRef    */   public final void decRef() throws IOException {     // only check refcount here (don't call ensureOpen()), so we can     // still close the reader if it was made invalid by a child:     if (refCount.get() <= 0) {       throw new AlreadyClosedException(\"this IndexReader is closed\");     }          final int rc = refCount.decrementAndGet();     if (rc == 0) {       closed = true;       Throwable throwable = null;       try {         doClose();       } catch (Throwable th) {         throwable = th;       } finally {         try {           reportCloseToParentReaders();         } finally {           notifyReaderClosedListeners(throwable);         }       }     } else if (rc < 0) {       throw new IllegalStateException(\"too many decRef calls: refCount is \" + rc + \" after decrement\");     }   }      /**    * Throws AlreadyClosedException if this IndexReader or any    * of its child readers is closed, otherwise returns.    */   protected final void ensureOpen() throws AlreadyClosedException {     if (refCount.get() <= 0) {       throw new AlreadyClosedException(\"this IndexReader is closed\");     }     // the happens before rule on reading the refCount, which must be after the fake write,     // ensures that we see the value:     if (closedByChild) {       throw new AlreadyClosedException(\"this IndexReader cannot be used anymore as one of its child readers was closed\");     }   }      /** {@inheritDoc}    * <p>For caching purposes, {@code IndexReader} subclasses are not allowed    * to implement equals/hashCode, so methods are declared final.    * To lookup instances from caches use {@link #getCoreCacheKey} and     * {@link #getCombinedCoreAndDeletesKey}.    */   @Override   public final boolean equals(Object obj) {     return (this == obj);   }      /** {@inheritDoc}    * <p>For caching purposes, {@code IndexReader} subclasses are not allowed    * to implement equals/hashCode, so methods are declared final.    * To lookup instances from caches use {@link #getCoreCacheKey} and     * {@link #getCombinedCoreAndDeletesKey}.    */   @Override   public final int hashCode() {     return System.identityHashCode(this);   }   /** Retrieve term vectors for this document, or null if    *  term vectors were not indexed.  The returned Fields    *  instance acts like a single-document inverted index    *  (the docID will be 0). */   public abstract Fields getTermVectors(int docID)           throws IOException;   /** Retrieve term vector for this document and field, or    *  null if term vectors were not indexed.  The returned    *  Fields instance acts like a single-document inverted    *  index (the docID will be 0). */   public final Terms getTermVector(int docID, String field)     throws IOException {     Fields vectors = getTermVectors(docID);     if (vectors == null) {       return null;     }     return vectors.terms(field);   }   /** Returns the number of documents in this index. */   public abstract int numDocs();   /** Returns one greater than the largest possible document number.    * This may be used to, e.g., determine how big to allocate an array which    * will have an element for every document number in an index.    */   public abstract int maxDoc();   /** Returns the number of deleted documents. */   public final int numDeletedDocs() {     return maxDoc() - numDocs();   }   /** Expert: visits the fields of a stored document, for    *  custom processing/loading of each field.  If you    *  simply want to load all fields, use {@link    *  #document(int)}.  If you want to load a subset, use    *  {@link DocumentStoredFieldVisitor}.  */   public abstract void document(int docID, StoredFieldVisitor visitor) throws IOException;      /**    * Returns the stored fields of the <code>n</code><sup>th</sup>    * <code>Document</code> in this index.  This is just    * sugar for using {@link DocumentStoredFieldVisitor}.    * <p>    * <b>NOTE:</b> for performance reasons, this method does not check if the    * requested document is deleted, and therefore asking for a deleted document    * may yield unspecified results. Usually this is not required, however you    * can test if the doc is deleted by checking the {@link    * Bits} returned from {@link MultiFields#getLiveDocs}.    *    * <b>NOTE:</b> only the content of a field is returned,    * if that field was stored during indexing.  Metadata    * like boost, omitNorm, IndexOptions, tokenized, etc.,    * are not preserved.    *     * @throws CorruptIndexException if the index is corrupt    * @throws IOException if there is a low-level IO error    */   // TODO: we need a separate StoredField, so that the   // Document returned here contains that class not   // IndexableField   public final Document document(int docID) throws IOException {     final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor();     document(docID, visitor);     return visitor.getDocument();   }   /**    * Like {@link #document(int)} but only loads the specified    * fields.  Note that this is simply sugar for {@link    * DocumentStoredFieldVisitor#DocumentStoredFieldVisitor(Set)}.    */   public final Document document(int docID, Set<String> fieldsToLoad)       throws IOException {     final DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor(         fieldsToLoad);     document(docID, visitor);     return visitor.getDocument();   }   /** Returns true if any documents have been deleted. Implementers should    *  consider overriding this method if {@link #maxDoc()} or {@link #numDocs()}    *  are not constant-time operations. */   public boolean hasDeletions() {     return numDeletedDocs() > 0;   }   /**    * Closes files associated with this index.    * Also saves any new deletions to disk.    * No other methods should be called after this has been called.    * @throws IOException if there is a low-level IO error    */   @Override   public final synchronized void close() throws IOException {     if (!closed) {       decRef();       closed = true;     }   }      /** Implements close. */   protected abstract void doClose() throws IOException;   /**    * Expert: Returns the root {@link IndexReaderContext} for this    * {@link IndexReader}'s sub-reader tree.     * <p>    * Iff this reader is composed of sub    * readers, i.e. this reader being a composite reader, this method returns a    * {@link CompositeReaderContext} holding the reader's direct children as well as a    * view of the reader tree's atomic leaf contexts. All sub-    * {@link IndexReaderContext} instances referenced from this readers top-level    * context are private to this reader and are not shared with another context    * tree. For example, IndexSearcher uses this API to drive searching by one    * atomic leaf reader at a time. If this reader is not composed of child    * readers, this method returns an {@link LeafReaderContext}.    * <p>    * Note: Any of the sub-{@link CompositeReaderContext} instances referenced    * from this top-level context do not support {@link CompositeReaderContext#leaves()}.    * Only the top-level context maintains the convenience leaf-view    * for performance reasons.    */   public abstract IndexReaderContext getContext();      /**    * Returns the reader's leaves, or itself if this reader is atomic.    * This is a convenience method calling {@code this.getContext().leaves()}.    * @see IndexReaderContext#leaves()    */   public final List<LeafReaderContext> leaves() {     return getContext().leaves();   }   /** Expert: Returns a key for this IndexReader, so CachingWrapperFilter can find    * it again.    * This key must not have equals()/hashCode() methods, so &quot;equals&quot; means &quot;identical&quot;. */   public Object getCoreCacheKey() {     // Don't call ensureOpen since FC calls this (to evict)     // on close     return this;   }   /** Expert: Returns a key for this IndexReader that also includes deletions,    * so CachingWrapperFilter can find it again.    * This key must not have equals()/hashCode() methods, so &quot;equals&quot; means &quot;identical&quot;. */   public Object getCombinedCoreAndDeletesKey() {     // Don't call ensureOpen since FC calls this (to evict)     // on close     return this;   }      /** Returns the number of documents containing the     * <code>term</code>.  This method returns 0 if the term or    * field does not exists.  This method does not take into    * account deleted documents that have not yet been merged    * away.     * @see TermsEnum#docFreq()    */   public abstract int docFreq(Term term) throws IOException;      /**    * Returns the total number of occurrences of {@code term} across all    * documents (the sum of the freq() for each doc that has this term). This    * will be -1 if the codec doesn't support this measure. Note that, like other    * term measures, this measure does not take deleted documents into account.    */   public abstract long totalTermFreq(Term term) throws IOException;      /**    * Returns the sum of {@link TermsEnum#docFreq()} for all terms in this field,    * or -1 if this measure isn't stored by the codec. Note that, just like other    * term measures, this measure does not take deleted documents into account.    *     * @see Terms#getSumDocFreq()    */   public abstract long getSumDocFreq(String field) throws IOException;      /**    * Returns the number of documents that have at least one term for this field,    * or -1 if this measure isn't stored by the codec. Note that, just like other    * term measures, this measure does not take deleted documents into account.    *     * @see Terms#getDocCount()    */   public abstract int getDocCount(String field) throws IOException;   /**    * Returns the sum of {@link TermsEnum#totalTermFreq} for all terms in this    * field, or -1 if this measure isn't stored by the codec (or if this fields    * omits term freq and positions). Note that, just like other term measures,    * this measure does not take deleted documents into account.    *     * @see Terms#getSumTotalTermFreq()    */   public abstract long getSumTotalTermFreq(String field) throws IOException; }\n";
        CodeAnalyzer codeAnalyzer=new CodeAnalyzer(code1);
        System.out.println(codeAnalyzer.getFullNameFromCode());


    }
}
